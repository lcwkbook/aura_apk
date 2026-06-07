package com.bitebiWePro;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class RunnerSupport {
    private static final String ROOT_RUN_DIR = "/data/local/tmp/wepro_runner";

    private RunnerSupport() {}

    public static File copyFromUri(Context context, Uri uri, String originalName) throws Exception {
        String name = displayFileName(originalName);
        File base = Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : context.getFilesDir();
        File dir = new File(base, "runner");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建临时运行目录: " + dir.getAbsolutePath());
        }
        File out = new File(dir, name);
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            if (input == null) {
                throw new Exception("无法读取所选文件");
            }
            output = new FileOutputStream(out, false);
            byte[] buf = new byte[8192];
            int len;
            while ((len = input.read(buf)) != -1) {
                output.write(buf, 0, len);
            }
            output.flush();
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
        }
        chmod777(out);
        return out;
    }


    public static File copyFromPath(Context context, File source, String originalName) throws Exception {
        if (source == null) throw new Exception("运行文件为空");
        String name = displayFileName(originalName == null || originalName.length() == 0 ? source.getName() : originalName);
        File base = Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : context.getFilesDir();
        File dir = new File(base, "runner");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建临时运行目录: " + dir.getAbsolutePath());
        }
        File out = new File(dir, name);
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(out, false);
            byte[] buf = new byte[8192];
            int len;
            while ((len = input.read(buf)) != -1) {
                output.write(buf, 0, len);
            }
            output.flush();
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
        }
        chmod777(out);
        return out;
    }

    public static void chmod777(File file) {
        try { file.setReadable(true, false); } catch (Exception ignored) {}
        try { file.setWritable(true, false); } catch (Exception ignored) {}
        try { file.setExecutable(true, false); } catch (Exception ignored) {}
        try {
            Process p = new ProcessBuilder("/system/bin/sh", "-c", "chmod 777 " + quote(file.getAbsolutePath())).start();
            waitFor(p, 2500);
        } catch (Exception ignored) {}
    }

    public static boolean hasRoot() {
        try {
            Process p = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
            if (!waitFor(p, 7000)) { try { p.destroy(); } catch (Exception ignored) {} return false; }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String prepareRootExecutable(File localFile, String originalName) throws Exception {
        chmod777(localFile);
        String rootName = displayFileName(originalName);
        String rootPath = ROOT_RUN_DIR + "/" + rootName;
        String tempPath = rootPath + ".tmp_" + System.currentTimeMillis();
        String cmd = "mkdir -p " + quote(ROOT_RUN_DIR)
                + " && rm -f " + quote(tempPath)
                + " && cat " + quote(localFile.getAbsolutePath()) + " > " + quote(tempPath)
                + " && chmod 777 " + quote(tempPath)
                + " && rm -f " + quote(rootPath)
                + " && mv " + quote(tempPath) + " " + quote(rootPath)
                + " && chmod 777 " + quote(rootPath)
                + " && rm -f " + quote(localFile.getAbsolutePath());
        CommandResult result = runSuCommand(cmd, 15000);
        if (result.exitCode != 0) {
            throw new Exception("root 移动到 /data/ 失败: " + result.output.trim());
        }
        return rootPath;
    }


    public static String prepareRootExecutableFromPath(String sourcePath, String originalName) throws Exception {
        if (sourcePath == null || sourcePath.length() == 0) throw new Exception("运行文件路径为空");
        String rootName = displayFileName(originalName);
        String rootPath = ROOT_RUN_DIR + "/" + rootName;
        String tempPath = rootPath + ".tmp_" + System.currentTimeMillis();
        String cmd = "mkdir -p " + quote(ROOT_RUN_DIR)
                + " && rm -f " + quote(tempPath)
                + " && cat " + quote(sourcePath) + " > " + quote(tempPath)
                + " && chmod 777 " + quote(tempPath)
                + " && rm -f " + quote(rootPath)
                + " && mv " + quote(tempPath) + " " + quote(rootPath)
                + " && chmod 777 " + quote(rootPath);
        CommandResult result = runSuCommand(cmd, 15000);
        if (result.exitCode != 0) {
            throw new Exception("root 复制运行文件失败: " + result.output.trim());
        }
        return rootPath;
    }

    public static CommandResult runSuCommand(String command, int timeoutMs) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        StreamCollector collector = new StreamCollector(p.getInputStream());
        collector.start();
        boolean finished = waitFor(p, timeoutMs);
        if (!finished) {
            try { p.destroy(); } catch (Exception ignored) {}
            throw new Exception("root 命令超时");
        }
        collector.join(800);
        return new CommandResult(p.exitValue(), collector.getText());
    }

    public static boolean isElf(File file) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] magic = new byte[4];
            int read = input.read(magic);
            return read == 4
                    && (magic[0] & 0xFF) == 0x7F
                    && magic[1] == 'E'
                    && magic[2] == 'L'
                    && magic[3] == 'F';
        } catch (Exception ignored) {
            return false;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
        }
    }

    public static String buildFileCommand(File file) {
        return buildFileCommand(file, false);
    }

    public static String buildFileCommand(File file, boolean isElf) {
        String path = file.getAbsolutePath();
        String lower = path.toLowerCase();
        String q = quote(path);
        String body;
        if (isElf) {
            body = buildElfBody(q);
        } else if (lower.endsWith(".sh")) {
            body = "sh " + q;
        } else if (lower.endsWith(".py")) {
            body = "if command -v python3 >/dev/null 2>&1; then python3 " + q
                    + "; elif command -v python >/dev/null 2>&1; then python " + q
                    + "; else echo '未找到 python/python3 解释器'; exit 127; fi";
        } else if (lower.endsWith(".js")) {
            body = "if command -v node >/dev/null 2>&1; then node " + q
                    + "; else echo '未找到 node 解释器'; exit 127; fi";
        } else {
            body = q;
        }
        File parent = file.getParentFile();
        if (parent != null) {
            return "cd " + quote(parent.getAbsolutePath()) + " && " + body;
        }
        return body;
    }

    private static String buildElfBody(String q) {
        return q
                + "; code=$?; "
                + "if [ $code -eq 126 ] || [ $code -eq 127 ]; then "
                + "if [ -x /system/bin/linker64 ]; then /system/bin/linker64 " + q + "; exit $?; "
                + "elif [ -x /system/bin/linker ]; then /system/bin/linker " + q + "; exit $?; "
                + "else exit $code; fi; "
                + "else exit $code; fi";
    }


    public static String buildDetachedFileCommand(File file, boolean isElf) {
        String path = file.getAbsolutePath();
        String lower = path.toLowerCase();
        String q = quote(path);
        String body;
        if (isElf) {
            body = buildElfBody(q);
        } else if (lower.endsWith(".sh")) {
            body = "/system/bin/sh " + q;
        } else if (lower.endsWith(".py")) {
            body = "if command -v python3 >/dev/null 2>&1; then python3 " + q
                    + "; elif command -v python >/dev/null 2>&1; then python " + q
                    + "; else exit 127; fi";
        } else if (lower.endsWith(".js")) {
            body = "if command -v node >/dev/null 2>&1; then node " + q
                    + "; else exit 127; fi";
        } else {
            body = q;
        }
        File parent = file.getParentFile();
        String cd = parent != null ? "cd " + quote(parent.getAbsolutePath()) + " && " : "";
        return "settings put global block_untrusted_touches 0 >/dev/null 2>&1 || true; "
                + cd
                + "( " + body + " ) >/dev/null 2>&1 < /dev/null & echo launched";
    }




    public static String buildInteractiveTerminalCommand(File file, boolean isElf) {
        String body = buildFileCommand(file, isElf);
        String shellBody = buildTouchCompatibilityCommand(null)
                + "export TERM=xterm; export HOME=/data/local/tmp; export TMPDIR=/data/local/tmp; "
                + "export PATH=/system/bin:/system/xbin:/vendor/bin:/vendor/xbin:/sbin:/data/local/tmp:$PATH; "
                + body;
        String qBody = quote(shellBody);
        return "if command -v script >/dev/null 2>&1; then "
                + "echo '[WePro] terminal mode: PTY(script)'; "
                + "script -q -c " + qBody + " /dev/null; "
                + "else echo '[WePro] terminal mode: pipe, script command not found'; "
                + "/system/bin/sh -c " + qBody + "; fi";
    }

    public static String buildRootShellCommand(File file, boolean isElf) {
        String terminal = buildInteractiveTerminalCommand(file, isElf);
        String path = file.getAbsolutePath();
        String script = "echo '[AuraKernel] Root shell connected'; "
                + "echo '[AuraKernel] Executing: " + shellEchoEscape(path) + "'; "
                + terminal
                + "; code=$?; echo; echo '[AuraKernel] exit code: '$code; exit $code";
        return "/system/bin/sh -c " + quote(script);
    }

    public static void stopRootProcessByPath(String path) {
        if (path == null || path.length() == 0) return;
        try {
            String base = baseName(path);
            String cmd = "pkill -f " + quote(path) + " >/dev/null 2>&1 || true; "
                    + "pid=$(pidof " + quote(base) + " 2>/dev/null); "
                    + "[ -n \"$pid\" ] && kill -9 $pid >/dev/null 2>&1 || true";
            runSuCommand(cmd, 2500);
        } catch (Exception ignored) {}
    }

    private static String shellEchoEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("'", "'\\''");
    }
    public static void applyTouchCompatibility(String packageName) {
        try {
            runSuCommand(buildTouchCompatibilityCommand(packageName), 4500);
        } catch (Exception ignored) {}
    }

    private static String buildTouchCompatibilityCommand(String packageName) {
        String cmd = "settings put global block_untrusted_touches 0 >/dev/null 2>&1 || true; "
                + "settings put secure block_untrusted_touches 0 >/dev/null 2>&1 || true; "
                + "settings put system block_untrusted_touches 0 >/dev/null 2>&1 || true; "
                + "setprop persist.sys.block_untrusted_touches 0 >/dev/null 2>&1 || true; ";
        if (packageName != null && packageName.length() > 0) {
            cmd += "if command -v appops >/dev/null 2>&1; then "
                    + "appops set " + quote(packageName) + " SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 || true; "
                    + "appops set " + quote(packageName) + " SYSTEM_ALERT_WINDOW 0 >/dev/null 2>&1 || true; "
                    + "fi; ";
        }
        return cmd;
    }

    public static boolean looksLikeElfOrBinaryName(String name) {
        if (name == null) return true;
        String lower = name.toLowerCase();
        return !(lower.endsWith(".sh") || lower.endsWith(".py") || lower.endsWith(".js")
                || lower.endsWith(".txt") || lower.endsWith(".zip") || lower.endsWith(".json")
                || lower.endsWith(".xml") || lower.endsWith(".ini") || lower.endsWith(".conf"));
    }

    public static String getKernelVersion() {
        try {
            CommandResult r = runShellCommand("uname -r", 3000);
            String v = r.output == null ? "" : r.output.trim();
            if (v.length() > 0) return firstLine(v);
        } catch (Exception ignored) {}
        try {
            String v = System.getProperty("os.version");
            if (v != null && v.trim().length() > 0) return firstLine(v.trim());
        } catch (Exception ignored) {}
        return "unknown";
    }


    public static String findMatchingDriverEntryName(File zipFile) throws Exception {
        if (zipFile == null) return null;
        String kernel = getKernelVersion();
        String kernelMain = kernel.split("[\\s-]+")[0];
        String nk = normalizeName(kernel);
        String nkm = normalizeName(kernelMain);
        InputStream raw = null;
        ZipInputStream zip = null;
        try {
            raw = new BufferedInputStream(new FileInputStream(zipFile));
            zip = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = baseName(entry.getName());
                if (name.length() == 0) continue;
                String lower = name.toLowerCase();
                String normalized = normalizeName(name);
                if (lower.contains(kernel.toLowerCase()) || normalized.contains(nk) || (nkm.length() > 0 && normalized.contains(nkm))) {
                    return entry.getName();
                }
            }
            return null;
        } finally {
            try { if (zip != null) zip.close(); } catch (Exception ignored) {}
            try { if (raw != null) raw.close(); } catch (Exception ignored) {}
        }
    }

    public static String findMatchingDriverEntryName(Context context, Uri zipUri) throws Exception {
        if (zipUri == null) return null;
        String kernel = getKernelVersion();
        String kernelMain = kernel.split("[\\s-]+")[0];
        String nk = normalizeName(kernel);
        String nkm = normalizeName(kernelMain);
        InputStream raw = null;
        ZipInputStream zip = null;
        try {
            raw = new BufferedInputStream(context.getContentResolver().openInputStream(zipUri));
            if (raw == null) throw new Exception("无法读取驱动 ZIP");
            zip = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = baseName(entry.getName());
                if (name.length() == 0) continue;
                String lower = name.toLowerCase();
                String normalized = normalizeName(name);
                if (lower.contains(kernel.toLowerCase()) || normalized.contains(nk) || (nkm.length() > 0 && normalized.contains(nkm))) {
                    return entry.getName();
                }
            }
            return null;
        } finally {
            try { if (zip != null) zip.close(); } catch (Exception ignored) {}
            try { if (raw != null) raw.close(); } catch (Exception ignored) {}
        }
    }


    public static String installDriverFromZip(Context context, File zipFile) throws Exception {
        String entryName = findMatchingDriverEntryName(zipFile);
        if (entryName == null || entryName.length() == 0) {
            throw new Exception("驱动 ZIP 中没有匹配当前内核版本的文件: " + getKernelVersion());
        }
        File local = extractDriverEntry(context, zipFile, entryName);
        chmod777(local);
        boolean isElf = isElf(local);
        String rootPath = prepareRootDriver(local, local.getName());
        CommandResult result = runSuCommand(buildDriverInstallCommand(new File(rootPath), isElf), 20000);
        if (result.exitCode != 0) {
            String out = result.output == null ? "" : result.output.trim();
            if (out.length() == 0) out = "返回码 " + result.exitCode;
            throw new Exception("驱动刷入失败: " + out);
        }
        return rootPath;
    }

    private static File extractDriverEntry(Context context, File zipFile, String entryName) throws Exception {
        File base = Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : context.getFilesDir();
        File dir = new File(base, "driver");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法创建驱动临时目录");
        String outName = displayFileName(baseName(entryName));
        File out = new File(dir, outName);
        InputStream raw = null;
        ZipInputStream zip = null;
        FileOutputStream output = null;
        try {
            raw = new BufferedInputStream(new FileInputStream(zipFile));
            zip = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (!entry.getName().equals(entryName)) continue;
                output = new FileOutputStream(out, false);
                byte[] buf = new byte[8192];
                int len;
                while ((len = zip.read(buf)) != -1) output.write(buf, 0, len);
                output.flush();
                return out;
            }
            throw new Exception("驱动条目不存在: " + entryName);
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (zip != null) zip.close(); } catch (Exception ignored) {}
            try { if (raw != null) raw.close(); } catch (Exception ignored) {}
        }
    }

    public static String installDriverFromZip(Context context, Uri zipUri) throws Exception {
        String entryName = findMatchingDriverEntryName(context, zipUri);
        if (entryName == null || entryName.length() == 0) {
            throw new Exception("驱动 ZIP 中没有匹配当前内核版本的文件: " + getKernelVersion());
        }
        File local = extractDriverEntry(context, zipUri, entryName);
        chmod777(local);
        boolean isElf = isElf(local);
        String rootPath = prepareRootDriver(local, local.getName());
        CommandResult result = runSuCommand(buildDriverInstallCommand(new File(rootPath), isElf), 20000);
        if (result.exitCode != 0) {
            String out = result.output == null ? "" : result.output.trim();
            if (out.length() == 0) out = "返回码 " + result.exitCode;
            throw new Exception("驱动刷入失败: " + out);
        }
        return rootPath;
    }

    private static File extractDriverEntry(Context context, Uri zipUri, String entryName) throws Exception {
        File base = Build.VERSION.SDK_INT >= 21 ? context.getCodeCacheDir() : context.getFilesDir();
        File dir = new File(base, "driver");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法创建驱动临时目录");
        String outName = displayFileName(baseName(entryName));
        File out = new File(dir, outName);
        InputStream raw = null;
        ZipInputStream zip = null;
        FileOutputStream output = null;
        try {
            raw = new BufferedInputStream(context.getContentResolver().openInputStream(zipUri));
            if (raw == null) throw new Exception("无法读取驱动 ZIP");
            zip = new ZipInputStream(raw);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (!entry.getName().equals(entryName)) continue;
                output = new FileOutputStream(out, false);
                byte[] buf = new byte[8192];
                int len;
                while ((len = zip.read(buf)) != -1) output.write(buf, 0, len);
                output.flush();
                return out;
            }
            throw new Exception("驱动条目不存在: " + entryName);
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (zip != null) zip.close(); } catch (Exception ignored) {}
            try { if (raw != null) raw.close(); } catch (Exception ignored) {}
        }
    }

    private static String prepareRootDriver(File localFile, String originalName) throws Exception {
        chmod777(localFile);
        String rootName = displayFileName(originalName);
        String rootPath = ROOT_RUN_DIR + "/" + rootName;
        String tempPath = rootPath + ".tmp_" + System.currentTimeMillis();
        String cmd = "mkdir -p " + quote(ROOT_RUN_DIR)
                + " && rm -f " + quote(tempPath)
                + " && cat " + quote(localFile.getAbsolutePath()) + " > " + quote(tempPath)
                + " && chmod 777 " + quote(tempPath)
                + " && rm -f " + quote(rootPath)
                + " && mv " + quote(tempPath) + " " + quote(rootPath)
                + " && chmod 777 " + quote(rootPath)
                + " && rm -f " + quote(localFile.getAbsolutePath());
        CommandResult result = runSuCommand(cmd, 15000);
        if (result.exitCode != 0) throw new Exception("驱动移动失败: " + result.output.trim());
        return rootPath;
    }

    private static String buildDriverInstallCommand(File rootFile, boolean isElf) {
        String path = rootFile.getAbsolutePath();
        String lower = path.toLowerCase();
        String q = quote(path);
        if (lower.endsWith(".sh")) {
            return "/system/bin/sh " + q + " >/dev/null 2>&1 < /dev/null";
        }
        if (lower.endsWith(".ko")) {
            return buildInsmodCommand(q);
        }
        if (isElf) {
            return q + " >/dev/null 2>&1 < /dev/null";
        }
        return buildInsmodOrExecCommand(q);
    }

    private static String buildInsmodOrExecCommand(String quotedPath) {
        return "out=$(insmod " + quotedPath + " 2>&1); code=$?; "
                + "if [ $code -eq 0 ]; then exit 0; fi; "
                + "echo \"$out\" | grep -qiE 'exist|already|busy' && exit 0; "
                + quotedPath + " >/dev/null 2>&1 < /dev/null";
    }

    private static String buildInsmodCommand(String quotedPath) {
        return "out=$(insmod " + quotedPath + " 2>&1); code=$?; "
                + "if [ $code -eq 0 ]; then exit 0; fi; "
                + "echo \"$out\" | grep -qiE 'exist|already|busy' && exit 0; "
                + "echo \"$out\"; exit $code";
    }

    private static CommandResult runShellCommand(String command, int timeoutMs) throws Exception {
        Process p = new ProcessBuilder("/system/bin/sh", "-c", command).redirectErrorStream(true).start();
        StreamCollector collector = new StreamCollector(p.getInputStream());
        collector.start();
        boolean finished = waitFor(p, timeoutMs);
        if (!finished) {
            try { p.destroy(); } catch (Exception ignored) {}
            throw new Exception("命令超时");
        }
        collector.join(800);
        return new CommandResult(p.exitValue(), collector.getText());
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        if (i >= 0) s = s.substring(0, i);
        return s.replace("\r", "").trim();
    }

    private static String baseName(String path) {
        if (path == null) return "";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String normalizeName(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) b.append(c);
        }
        return b.toString();
    }

    public static String displayFileName(String name) {
        if (name == null) name = "";
        name = name.trim();
        if (name.length() == 0) name = "selected_file";
        name = name.replace('\u0000', '_').replace('\r', '_').replace('\n', '_');
        name = name.replace('/', '_').replace('\\', '_');
        if (name.equals(".") || name.equals("..")) name = "selected_file";
        if (name.length() > 160) name = name.substring(name.length() - 160);
        return name;
    }

    public static String safeFileName(String name) {
        return displayFileName(name);
    }

    public static String quote(String s) {
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') b.append("'\\''");
            else b.append(c);
        }
        b.append("'");
        return b.toString();
    }

    private static boolean waitFor(Process p, int timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                p.exitValue();
                return true;
            } catch (IllegalThreadStateException running) {
                try { Thread.sleep(60); } catch (InterruptedException e) { return false; }
            }
        }
        return false;
    }

    public static final class CommandResult {
        public final int exitCode;
        public final String output;
        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }

    private static final class StreamCollector extends Thread {
        private final InputStream input;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        StreamCollector(InputStream input) {
            this.input = input;
        }

        public void run() {
            byte[] buf = new byte[1024];
            int len;
            try {
                while ((len = input.read(buf)) != -1) {
                    output.write(buf, 0, len);
                }
            } catch (Exception ignored) {
            } finally {
                try { input.close(); } catch (Exception ignored) {}
            }
        }

        String getText() {
            try { return output.toString("UTF-8"); } catch (Exception e) { return output.toString(); }
        }
    }
}
