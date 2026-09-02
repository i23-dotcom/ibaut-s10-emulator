#include <jni.h>
#include <android/log.h>

#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>

#include <string>
#include <vector>

#define LOG_TAG "ibaut-QEMU"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static pid_t qemuPid = -1;

static std::string jstringToString(
        JNIEnv *env,
        jstring value) {

    if (value == nullptr) {
        return "";
    }

    const char *chars =
            env->GetStringUTFChars(value, nullptr);

    if (chars == nullptr) {
        return "";
    }

    std::string result(chars);

    env->ReleaseStringUTFChars(
            value,
            chars
    );

    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ibaut_s10emulator_MainActivity_nativeLaunchQemu(
        JNIEnv *env,
        jobject,
        jstring isoPath,
        jstring diskPath,
        jint ramMb,
        jint cpuCount) {

    if (qemuPid > 0) {
        LOGE("QEMU is already running: %d", qemuPid);
        return -2;
    }

    std::string iso =
            jstringToString(env, isoPath);

    std::string disk =
            jstringToString(env, diskPath);

    if (iso.empty() || disk.empty()) {
        LOGE("ISO or disk path is empty");
        return -3;
    }

    /*
     * QEMU executable location.
     *
     * The Android application will place the
     * Android-compatible QEMU executable here.
     */
    const char *qemu =
            "/data/data/com.ibaut.s10emulator/files/qemu-system-aarch64";

    std::string memory =
            std::to_string(
                    static_cast<int>(ramMb)
            ) + "M";

    std::string cpus =
            std::to_string(
                    static_cast<int>(cpuCount)
            );

    pid_t pid = fork();

    if (pid < 0) {

        LOGE("fork() failed");

        return -4;
    }

    if (pid == 0) {

        /*
         * Child process.
         */

        std::vector<std::string> args;

        args.emplace_back(qemu);

        args.emplace_back("-machine");
        args.emplace_back("virt");

        args.emplace_back("-cpu");
        args.emplace_back("max");

        args.emplace_back("-smp");
        args.emplace_back(cpus);

        args.emplace_back("-m");
        args.emplace_back(memory);

        args.emplace_back("-drive");
        args.emplace_back(
                "file=" + disk +
                ",format=raw,if=virtio"
        );

        args.emplace_back("-cdrom");
        args.emplace_back(iso);

        args.emplace_back("-boot");
        args.emplace_back("order=d");

        /*
         * Serial console.
         */
        args.emplace_back("-serial");
        args.emplace_back("stdio");

        /*
         * Don't automatically terminate the
         * guest when the Android process exits.
         */
        args.emplace_back("-no-reboot");

        std::vector<char *> argv;

        for (auto &arg : args) {
            argv.push_back(
                    const_cast<char *>(arg.c_str())
            );
        }

        argv.push_back(nullptr);

        LOGI(
                "Launching QEMU: %s",
                qemu
        );

        execv(
                qemu,
                argv.data()
        );

        /*
         * execv() only returns when it fails.
         */
        LOGE(
                "execv() failed"
        );

        _exit(127);
    }

    /*
     * Parent Android process.
     */

    qemuPid = pid;

    LOGI(
            "QEMU started with PID %d",
            qemuPid
    );

    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ibaut_s10emulator_MainActivity_nativeStopQemu(
        JNIEnv *,
        jobject) {

    if (qemuPid <= 0) {

        LOGI("No QEMU process running");

        return;
    }

    LOGI(
            "Stopping QEMU PID %d",
            qemuPid
    );

    /*
     * Ask QEMU to terminate.
     */
    kill(
            qemuPid,
            SIGTERM
    );

    /*
     * Give it a short opportunity to exit.
     */
    usleep(500000);

    /*
     * Check whether it is still alive.
     */
    int status = 0;

    pid_t result =
            waitpid(
                    qemuPid,
                    &status,
                    WNOHANG
            );

    if (result == 0) {

        /*
         * QEMU did not exit.
         */
        kill(
                qemuPid,
                SIGKILL
        );

        waitpid(
                qemuPid,
                &status,
                0
        );
    }

    qemuPid = -1;

    LOGI("QEMU stopped");
}
