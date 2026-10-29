package tr.hizlitv.home;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses PSS only: RSS contains shared pages and must not be added per app. */
public final class MemorySnapshot {
    public static final class Entry {
        public final int pid;
        public final String name;
        public long pssKb;
        public String state = "Unknown";
        Entry(int pid, String name, long kb) { this.pid = pid; this.name = name; pssKb = kb; }
    }
    public final List<Entry> entries = new ArrayList<>();
    private static final Pattern ROW = Pattern.compile("^\\s*([0-9,]+)K:\\s+(.+?)\\s+\\(pid (\\d+)[^)]*\\).*$");
    private static final Pattern GROUP = Pattern.compile("^\\s*[0-9,]+K:\\s+([^()]+?)\\s*$");

    public static MemorySnapshot parse(String text) {
        if (!text.contains("Total PSS by process:")) return parseActivityProcesses(text);
        MemorySnapshot result = new MemorySnapshot();
        Map<Integer, Entry> byPid = new LinkedHashMap<>();
        int section = 0;
        String state = "Unknown";
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith("Total PSS by process:")) { section = 1; continue; }
            if (line.startsWith("Total PSS by OOM adjustment:")) { section = 2; continue; }
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))) { section = 0; continue; }
            if (section == 0) continue;
            Matcher row = ROW.matcher(line);
            if (row.matches()) {
                int pid = Integer.parseInt(row.group(3));
                if (section == 1) {
                    if (!byPid.containsKey(pid)) byPid.put(pid,
                        new Entry(pid, row.group(2), Long.parseLong(row.group(1).replace(",", ""))));
                } else if (byPid.containsKey(pid)) byPid.get(pid).state = state;
            } else if (section == 2) {
                Matcher group = GROUP.matcher(line);
                if (group.matches()) state = group.group(1);
            }
        }
        result.entries.addAll(byPid.values());
        return result;
    }

    private static MemorySnapshot parseActivityProcesses(String text) {
        MemorySnapshot result = new MemorySnapshot();
        Map<Integer, Entry> byPid = new LinkedHashMap<>();
        Pattern header = Pattern.compile("^\\s*\\*\\w+\\*\\s+UID\\s+\\d+\\s+ProcessRecord\\{\\S+\\s+(\\d+):([^/\\s}]+)/.*$");
        Pattern pss = Pattern.compile("(?:^|\\s)lastPss=([0-9.,]+)([A-Za-z]*)");
        Pattern procState = Pattern.compile("(?:^|\\s)setProcState=(\\d+)");
        Entry current = null;
        for (String line : text.split("\\r?\\n")) {
            Matcher start = header.matcher(line);
            if (start.matches()) {
                int pid = Integer.parseInt(start.group(1));
                current = new Entry(pid, start.group(2), 0);
                byPid.put(pid, current); continue;
            }
            if (!line.trim().isEmpty() && !line.startsWith("    ")) { current = null; continue; }
            if (current == null) continue;
            Matcher memory = pss.matcher(line);
            if (memory.find()) {
                double value = Double.parseDouble(memory.group(1).replace(',', '.'));
                String unit = memory.group(2);
                if (unit.equals("GB")) value *= 1024 * 1024;
                else if (unit.equals("MB")) value *= 1024;
                else if (!unit.equals("KB")) value /= 1024;
                current.pssKb = Math.round(value);
            }
            Matcher state = procState.matcher(line);
            if (state.find()) {
                int code = Integer.parseInt(state.group(1));
                current.state = code >= 16 ? "Cached" : code == 2 ? "Foreground" : code <= 8 ? "Perceptible" : "Service";
            }
        }
        result.entries.addAll(byPid.values()); return result;
    }

    public List<Entry> forPackage(String pkg) {
        List<Entry> found = new ArrayList<>();
        for (Entry entry : entries) if (entry.name.equals(pkg) || entry.name.startsWith(pkg + ":")) found.add(entry);
        return found;
    }
    public static boolean allCached(List<Entry> entries) {
        if (entries.isEmpty()) return false;
        for (Entry e : entries) if (!e.state.equals("Cached")) return false;
        return true;
    }
}
