package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One reading for the visible recent-app cards, using the existing loopback ADB connection. */
final class RecentAppMetrics {
    private static final Pattern CPU_ROW = Pattern.compile("^\\s*(\\d+)\\s+(\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern PSS_ROW = Pattern.compile("^\\s+([\\d,]+)K:\\s+.+?\\(pid\\s+(\\d+)(?:\\s|\\)).*$");

    final Double cpu;
    final Long ram;
    final Long storage;

    private RecentAppMetrics(Double cpu, Long ram, Long storage) {
        this.cpu = cpu;
        this.ram = ram;
        this.storage = storage;
    }

    static Map<String, RecentAppMetrics> read(Context context, List<String> packages, int user) {
        Map<String, RecentAppMetrics> result = new HashMap<>();
        if (packages.isEmpty()) return result;
        Map<Integer, Double> cpuByPid = cpu(readOrEmpty(LocalCecReader::sampleAppCpu));
        Map<Integer, String> owners = processOwners(context.getPackageManager(), packages,
                readOrEmpty(LocalCecReader::readAppProcesses));
        Map<Integer, Long> ramByPid = pss(readOrEmpty(LocalCecReader::readAppMemory));
        Map<String, Long> disk = user == 0 ? storage(readOrEmpty(LocalCecReader::readAppStorage)) : new HashMap<>();
        Map<String, Double> cpu = new HashMap<>();
        Map<String, Long> ram = new HashMap<>();
        for (Map.Entry<Integer, String> process : owners.entrySet()) {
            Double usage = cpuByPid.get(process.getKey());
            Long pss = ramByPid.get(process.getKey());
            if (usage != null) cpu.merge(process.getValue(), usage, Double::sum);
            if (pss != null) ram.merge(process.getValue(), pss, Long::sum);
        }
        for (String pkg : packages) result.put(pkg, new RecentAppMetrics(cpu.get(pkg), ram.get(pkg), disk.get(pkg)));
        return result;
    }

    private static Map<Integer, String> processOwners(PackageManager pm, List<String> packages, String listing) {
        Map<String, Integer> appUids = new HashMap<>();
        Map<Integer, String> uniqueUids = new HashMap<>();
        for (String pkg : packages) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                appUids.put(pkg, info.uid);
                String[] shared = pm.getPackagesForUid(info.uid);
                if (shared != null && shared.length == 1 && pkg.equals(shared[0])) uniqueUids.put(info.uid, pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Uninstalled since the recent-task query.
            }
        }
        Map<Integer, String> result = new HashMap<>();
        for (String row : listing.split("\\R")) {
            String[] fields = row.trim().split("\\s+", 3);
            if (fields.length != 3) continue;
            try {
                int uid = Integer.parseInt(fields[0]);
                int pid = Integer.parseInt(fields[1]);
                String name = fields[2].split(":", 2)[0];
                String pkg = Integer.valueOf(uid).equals(appUids.get(name)) ? name : uniqueUids.get(uid);
                if (pkg != null) result.put(pid, pkg);
            } catch (NumberFormatException ignored) {
                // Header, or a firmware without numeric UIDs in this output.
            }
        }
        return result;
    }

    static Map<Integer, Double> cpu(String dump) {
        int samples = 0;
        Map<Integer, Double> values = new HashMap<>();
        for (String line : dump.split("\\R")) {
            if (line.trim().startsWith("PID") && line.contains("%CPU")) {
                values.clear();
                samples++;
            }
            if (samples < 2) continue;
            Matcher row = CPU_ROW.matcher(line);
            if (row.matches()) {
                try {
                    double percentage = Double.parseDouble(row.group(2));
                    if (Double.isFinite(percentage)) values.put(Integer.parseInt(row.group(1)), percentage);
                } catch (NumberFormatException ignored) {}
            }
        }
        return values;
    }

    static Map<Integer, Long> pss(String dump) {
        Map<Integer, Long> values = new HashMap<>();
        boolean inPss = false;
        for (String line : dump.split("\\R")) {
            if (line.startsWith("Total PSS by OOM adjustment:")) {
                inPss = true;
                continue;
            }
            if (!inPss || line.isEmpty()) continue;
            if (!Character.isWhitespace(line.charAt(0))) break;
            Matcher row = PSS_ROW.matcher(line);
            if (row.matches()) {
                try {
                    long kib = Long.parseLong(row.group(1).replace(",", ""));
                    values.put(Integer.parseInt(row.group(2)), Math.multiplyExact(kib, 1024L));
                } catch (NumberFormatException | ArithmeticException ignored) {}
            }
        }
        return values;
    }

    private static Map<String, Long> storage(String dump) {
        Map<String, Long> result = new HashMap<>();
        try {
            JSONArray names = array(dump, "Package Names: ");
            JSONArray sizes = array(dump, "App Sizes: ");
            JSONArray data = array(dump, "App Data Sizes: ");
            if (names.length() != sizes.length() || names.length() != data.length()) return result;
            for (int i = 0; i < names.length(); i++) {
                long code = sizes.optLong(i, -1);
                long files = data.optLong(i, -1);
                // Android includes cache in app data.
                if (code >= 0 && files >= 0 && code <= Long.MAX_VALUE - files) {
                    result.put(names.getString(i), code + files);
                }
            }
        } catch (JSONException ignored) {
            // No diskstats cache or a different format.
        }
        return result;
    }

    private static JSONArray array(String dump, String prefix) throws JSONException {
        for (String line : dump.split("\\R")) {
            if (line.startsWith(prefix)) return new JSONArray(line.substring(prefix.length()));
        }
        return new JSONArray();
    }

    private interface Reading { String read() throws Exception; }

    private static String readOrEmpty(Reading reading) {
        if (Thread.currentThread().isInterrupted()) return "";
        try {
            return reading.read();
        } catch (Exception ignored) {
            return "";
        }
    }
}
