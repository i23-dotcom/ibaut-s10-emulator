package com.ibaut.s10emulator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    companion object {
        private const val ISO_PICKER = 1001

        init {
            try {
                System.loadLibrary("ibaut_vm_bridge")
            } catch (_: UnsatisfiedLinkError) {
                // Native QEMU library will be supplied by the build system.
            }
        }
    }

    private lateinit var statusText: TextView
    private lateinit var isoText: TextView

    private var selectedIso: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()
        updateStatus("Ready — select a bootable ARM64 ISO.")
    }

    private fun createInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(0xFF080808.toInt())
        }

        val title = TextView(this).apply {
            text = "ibaut S10+ Emulator"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val subtitle = TextView(this).apply {
            text = "ARM64 Virtual Machine"
            textSize = 16f
            setTextColor(0xFFAAAAAA.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        isoText = TextView(this).apply {
            text = "ISO: No ISO selected"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(10, 20, 10, 20)
        }

        val selectIso = makeButton("SELECT ISO")

        selectIso.setOnClickListener {
            openIsoPicker()
        }

        val createDisk = makeButton("CREATE 32 GB VIRTUAL DISK")

        createDisk.setOnClickListener {
            val disk = createVirtualDisk()

            if (disk != null) {
                updateStatus(
                    "Virtual disk ready:\n${disk.absolutePath}"
                )
            }
        }

        val startVm = makeButton("START VIRTUAL MACHINE")

        startVm.setOnClickListener {
            startVirtualMachine()
        }

        val stopVm = makeButton("STOP VIRTUAL MACHINE")

        stopVm.setOnClickListener {
            stopVirtualMachine()
        }

        val settings = makeButton("VM SETTINGS")

        settings.setOnClickListener {
            Toast.makeText(
                this,
                "VM settings will be added to the next stage.",
                Toast.LENGTH_SHORT
            ).show()
        }

        statusText = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFCCCCCC.toInt())
            setPadding(10, 30, 10, 30)
        }

        val info = TextView(this).apply {
            text = """
                VM configuration

                Architecture: ARM64
                CPU: 4 virtual CPUs
                RAM: 4096 MB
                Machine: QEMU virt
                Storage: 32 GB
                Boot media: ISO

                This emulator uses QEMU system
                emulation rather than pretending to
                be a phone interface.
            """.trimIndent()

            textSize = 14f
            setTextColor(0xFF888888.toInt())
            setPadding(10, 30, 10, 30)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(isoText)
        root.addView(selectIso)
        root.addView(createDisk)
        root.addView(startVm)
        root.addView(stopVm)
        root.addView(settings)
        root.addView(statusText)
        root.addView(info)

        val scroll = ScrollView(this)
        scroll.addView(root)

        setContentView(scroll)
    }

    private fun makeButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF252525.toInt())

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }
    }

    private fun openIsoPicker() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, ISO_PICKER)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != ISO_PICKER ||
            resultCode != RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val uri = data.data!!

        try {

            val isoFile = File(
                filesDir,
                "isos/guest.iso"
            )

            isoFile.parentFile?.mkdirs()

            contentResolver.openInputStream(uri).use { input ->

                if (input == null) {
                    throw Exception("Unable to open ISO")
                }

                FileOutputStream(isoFile).use { output ->

                    val buffer = ByteArray(1024 * 1024)

                    while (true) {
                        val count = input.read(buffer)

                        if (count <= 0) break

                        output.write(buffer, 0, count)
                    }

                    output.flush()
                }
            }

            selectedIso = isoFile

            isoText.text =
                "ISO: ${isoFile.name}\nSize: ${formatSize(isoFile.length())}"

            updateStatus(
                "ISO copied into emulator storage."
            )

            Toast.makeText(
                this,
                "ISO selected successfully",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            updateStatus(
                "ISO error: ${e.message}"
            )
        }
    }

    private fun createVirtualDisk(): File? {

        return try {

            val directory = File(
                filesDir,
                "vm"
            )

            directory.mkdirs()

            val disk = File(
                directory,
                "s10plus-disk.img"
            )

            if (!disk.exists()) {

                RandomAccessFileCompat.createSparseFile(
                    disk,
                    32L * 1024L * 1024L * 1024L
                )
            }

            updateStatus(
                "32 GB virtual disk created."
            )

            disk

        } catch (e: Exception) {

            updateStatus(
                "Disk error: ${e.message}"
            )

            null
        }
    }

    private fun startVirtualMachine() {

        val iso = selectedIso

        if (iso == null || !iso.exists()) {

            Toast.makeText(
                this,
                "Select an ISO first.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val disk = createVirtualDisk()

        if (disk == null) {
            return
        }

        updateStatus(
            """
            Starting QEMU...

            Architecture: ARM64
            CPU: 4
            RAM: 4096 MB
            ISO: ${iso.name}
            Disk: ${disk.name}
            """.trimIndent()
        )

        try {

            val result = nativeLaunchQemu(
                iso.absolutePath,
                disk.absolutePath,
                4096,
                4
            )

            updateStatus(
                "QEMU result: $result"
            )

        } catch (e: UnsatisfiedLinkError) {

            updateStatus(
                "QEMU native engine is not included in this APK yet."
            )

        } catch (e: Exception) {

            updateStatus(
                "Unable to start VM: ${e.message}"
            )
        }
    }

    private fun stopVirtualMachine() {

        try {

            nativeStopQemu()

            updateStatus(
                "Virtual machine stopped."
            )

        } catch (_: Throwable) {

            updateStatus(
                "No running QEMU process."
            )
        }
    }

    private fun updateStatus(message: String) {
        if (::statusText.isInitialized) {
            statusText.text = message
        }
    }

    private fun formatSize(bytes: Long): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        if (bytes < 1024 * 1024) {
            return "${bytes / 1024} KB"
        }

        if (bytes < 1024 * 1024 * 1024) {
            return "${bytes / (1024 * 1024)} MB"
        }

        return "${bytes / (1024 * 1024 * 1024)} GB"
    }

    private external fun nativeLaunchQemu(
        isoPath: String,
        diskPath: String,
        ramMb: Int,
        cpuCount: Int
    ): Int

    private external fun nativeStopQemu()
}

private object RandomAccessFileCompat {

    fun createSparseFile(
        file: File,
        size: Long
    ) {

        file.parentFile?.mkdirs()

        java.io.RandomAccessFile(
            file,
            "rw"
        ).use { raf ->

            raf.setLength(size)
        }
    }
}
