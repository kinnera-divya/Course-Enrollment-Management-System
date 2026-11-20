package courseenrollment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileManager {
    private static final String STUDENTS = "students.txt";
    private static final String COURSES = "courses.txt";
    private static final String ENROLLMENTS = "enrollments.txt";
    private static final String REPORT = "report.txt";

    private final Path dir;

    public FileManager(String folder) {
        this.dir = Paths.get(folder);
    }

    public void init() throws IOException {
        Files.createDirectories(dir);
    }

    /** Required text: not blank, no '|' and no line breaks. */
    public static boolean isValidText(String s) {
        return s != null && !s.isBlank()
                && s.indexOf('|') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0;
    }

    // ---------- loading ----------

    public List<Student> loadStudents() throws IOException {
        List<Student> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        List<String> lines = readLines(STUDENTS);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = line.split("\\|", -1);
            if (f.length != 3) throw bad(STUDENTS, n, "expected 3 fields");
            int id = parsePositive(f[0], STUDENTS, n, "ID");
            if (!ids.add(id)) throw bad(STUDENTS, n, "duplicate ID " + id);
            try {
                list.add(new Student(id, f[1], f[2]));
            } catch (IllegalArgumentException e) {
                throw bad(STUDENTS, n, e.getMessage());
            }
        }
        return list;
    }

    public List<Course> loadCourses() throws IOException {
        List<Course> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        List<String> lines = readLines(COURSES);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = line.split("\\|", -1);
            if (f.length != 5) throw bad(COURSES, n, "expected 5 fields");
            int id = parsePositive(f[0], COURSES, n, "ID");
            if (!ids.add(id)) throw bad(COURSES, n, "duplicate ID " + id);
            int capacity = parsePositive(f[4], COURSES, n, "capacity");
            try {
                list.add(new Course(id, f[1], f[2], f[3], capacity));
            } catch (IllegalArgumentException e) {
                throw bad(COURSES, n, e.getMessage());
            }
        }
        return list;
    }

    public List<Enrollment> loadEnrollments(List<Student> students, List<Course> courses) throws IOException {
        List<Enrollment> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> activePairs = new HashSet<>();
        List<String> lines = readLines(ENROLLMENTS);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int n = i + 1;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) throw bad(ENROLLMENTS, n, "expected 4 fields");
            int id = parsePositive(f[0], ENROLLMENTS, n, "ID");
            int studentId = parsePositive(f[1], ENROLLMENTS, n, "student ID");
            int courseId = parsePositive(f[2], ENROLLMENTS, n, "course ID");
            if (!ids.add(id)) throw bad(ENROLLMENTS, n, "duplicate ID " + id);
            EnrollmentStatus status;
            try {
                status = EnrollmentStatus.valueOf(f[3]);
            } catch (IllegalArgumentException e) {
                throw bad(ENROLLMENTS, n, "status must be ACTIVE or CANCELLED");
            }
            if (!hasStudent(students, studentId)) throw bad(ENROLLMENTS, n, "unknown student ID " + studentId);
            if (!hasCourse(courses, courseId)) throw bad(ENROLLMENTS, n, "unknown course ID " + courseId);
            if (status == EnrollmentStatus.ACTIVE && !activePairs.add(studentId + "-" + courseId)) {
                throw bad(ENROLLMENTS, n, "duplicate ACTIVE enrollment for this student and course");
            }
            list.add(new Enrollment(id, studentId, courseId, status));
        }
        // capacity check
        for (Course c : courses) {
            int count = 0;
            for (Enrollment e : list) {
                if (e.getCourseId() == c.getId() && e.getStatus() == EnrollmentStatus.ACTIVE) count++;
            }
            if (count > c.getCapacity()) {
                throw new IOException(ENROLLMENTS + ": course " + c.getId() + " has more ACTIVE enrollments than capacity");
            }
        }
        return list;
    }

    // ---------- saving ----------

    public void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Student s : students) {
            lines.add(s.getId() + "|" + s.getName() + "|" + s.getDepartment());
        }
        writeAtomic(STUDENTS, lines);
    }

    public void saveCourses(List<Course> courses) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Course c : courses) {
            lines.add(c.getId() + "|" + c.getTitle() + "|" + c.getTrainer() + "|" + c.getBatch() + "|" + c.getCapacity());
        }
        writeAtomic(COURSES, lines);
    }

    public void saveEnrollments(List<Enrollment> enrollments) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Enrollment e : enrollments) {
            lines.add(e.getId() + "|" + e.getStudentId() + "|" + e.getCourseId() + "|" + e.getStatus());
        }
        writeAtomic(ENROLLMENTS, lines);
    }

    public void saveReport(List<String> lines) throws IOException {
        writeAtomic(REPORT, lines);
    }

    // ---------- helpers ----------

    private List<String> readLines(String file) throws IOException {
        List<String> lines = new ArrayList<>();
        Path p = dir.resolve(file);
        if (!Files.exists(p)) return lines; // first run: empty list
        try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            throw new IOException("Cannot read " + file + ": " + e.getMessage(), e);
        }
        return lines;
    }

    private void writeAtomic(String file, List<String> lines) throws IOException {
        Path target = dir.resolve(file);
        Path temp = dir.resolve(file + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                w.write(line);
                w.newLine();
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static int parsePositive(String text, String file, int line, String field) throws IOException {
        try {
            int v = Integer.parseInt(text.trim());
            if (v < 1) throw bad(file, line, field + " must be a positive number");
            return v;
        } catch (NumberFormatException e) {
            throw bad(file, line, field + " must be a number");
        }
    }

    private static IOException bad(String file, int line, String msg) {
        return new IOException(file + ", line " + line + ": " + msg);
    }

    private static boolean hasStudent(List<Student> list, int id) {
        for (Student s : list) if (s.getId() == id) return true;
        return false;
    }

    private static boolean hasCourse(List<Course> list, int id) {
        for (Course c : list) if (c.getId() == id) return true;
        return false;
    }
}
