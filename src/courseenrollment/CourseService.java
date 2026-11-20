package courseenrollment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CourseService {
    private final FileManager files;
    private final List<Student> students;
    private final List<Course> courses;
    private final List<Enrollment> enrollments;
    private int nextStudentId;
    private int nextCourseId;
    private int nextEnrollmentId;

    /** Loads all files into temporary lists; accepts them only if every record is valid. */
    public CourseService(FileManager files) throws IOException {
        this.files = files;
        files.init();
        List<Student> s = files.loadStudents();
        List<Course> c = files.loadCourses();
        List<Enrollment> e = files.loadEnrollments(s, c);
        this.students = s;
        this.courses = c;
        this.enrollments = e;

        int maxS = 0, maxC = 0, maxE = 0;
        for (Student st : students) maxS = Math.max(maxS, st.getId());
        for (Course co : courses) maxC = Math.max(maxC, co.getId());
        for (Enrollment en : enrollments) maxE = Math.max(maxE, en.getId());
        nextStudentId = maxS + 1;
        nextCourseId = maxC + 1;
        nextEnrollmentId = maxE + 1;
    }

    // ---------- lookups ----------

    public Student findStudent(int id) {
        for (Student s : students) if (s.getId() == id) return s;
        return null;
    }

    public Course findCourse(int id) {
        for (Course c : courses) if (c.getId() == id) return c;
        return null;
    }

    public Enrollment findEnrollment(int id) {
        for (Enrollment e : enrollments) if (e.getId() == id) return e;
        return null;
    }

    public int countActive(int courseId) {
        int count = 0;
        for (Enrollment e : enrollments) {
            if (e.getCourseId() == courseId && e.getStatus() == EnrollmentStatus.ACTIVE) count++;
        }
        return count;
    }

    private boolean hasActive(int studentId, int courseId) {
        for (Enrollment e : enrollments) {
            if (e.getStudentId() == studentId && e.getCourseId() == courseId
                    && e.getStatus() == EnrollmentStatus.ACTIVE) return true;
        }
        return false;
    }

    // ---------- actions ----------

    public Student registerStudent(String name, String department) throws IOException {
        Student s = new Student(nextStudentId, name, department); // validates
        students.add(s);
        try {
            files.saveStudents(students);
        } catch (IOException e) {
            students.remove(s);
            throw e;
        }
        nextStudentId++;
        return s;
    }

    public Course addCourse(String title, String trainer, int batchChoice, int capacity) throws IOException {
        String[] batches = Course.getBatches();
        if (batchChoice < 1 || batchChoice > batches.length) {
            throw new IllegalArgumentException("Batch choice must be 1 to " + batches.length + ".");
        }
        Course c = new Course(nextCourseId, title, trainer, batches[batchChoice - 1], capacity);
        courses.add(c);
        try {
            files.saveCourses(courses);
        } catch (IOException e) {
            courses.remove(c);
            throw e;
        }
        nextCourseId++;
        return c;
    }

    public Enrollment enroll(int studentId, int courseId) throws IOException {
        Course course = findCourse(courseId);
        if (course == null) throw new IllegalArgumentException("Course ID " + courseId + " not found.");
        if (hasActive(studentId, courseId)) throw new IllegalArgumentException("You already have an active enrollment.");
        if (countActive(courseId) >= course.getCapacity()) throw new IllegalArgumentException("This course is full.");

        Enrollment e = new Enrollment(nextEnrollmentId, studentId, courseId, EnrollmentStatus.ACTIVE);
        enrollments.add(e);
        try {
            files.saveEnrollments(enrollments);
        } catch (IOException ex) {
            enrollments.remove(e);
            throw ex;
        }
        nextEnrollmentId++;
        return e;
    }

    public Enrollment cancelEnrollment(int studentId, int enrollmentId) throws IOException {
        Enrollment e = findEnrollment(enrollmentId);
        if (e == null || e.getStudentId() != studentId || e.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new IllegalArgumentException("No active enrollment with ID " + enrollmentId + " found for you.");
        }
        e.cancel();
        try {
            files.saveEnrollments(enrollments);
        } catch (IOException ex) {
            e.undoCancel();
            throw ex;
        }
        return e;
    }

    // ---------- display ----------

    /** batch == null shows all courses. */
    public void printCourses(String batch) {
        boolean any = false;
        for (Course c : courses) {
            if (batch == null || c.getBatch().equals(batch)) {
                c.showDetails(countActive(c.getId()));
                any = true;
            }
        }
        if (!any) System.out.println("No records found.");
    }

    public void printStudents() {
        if (students.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Student s : students) {
            User u = s;          // polymorphism: calls Student's showProfile()
            u.showProfile();
        }
    }

    /** studentId == null shows every enrollment. */
    public void printEnrollments(Integer studentId) {
        boolean any = false;
        for (Enrollment e : enrollments) {
            if (studentId != null && e.getStudentId() != studentId) continue;
            Course c = findCourse(e.getCourseId());
            System.out.println("Enrollment " + e.getId() + " | Student " + e.getStudentId()
                    + " | Course " + e.getCourseId() + " (" + c.getTitle() + ") | " + e.getStatus());
            any = true;
        }
        if (!any) System.out.println("No records found.");
    }

    public List<String> buildReport() {
        int active = 0, cancelled = 0;
        Set<Integer> activeStudents = new HashSet<>();
        for (Enrollment e : enrollments) {
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                active++;
                activeStudents.add(e.getStudentId());
            } else {
                cancelled++;
            }
        }
        List<String> r = new ArrayList<>();
        r.add("Total students: " + students.size());
        r.add("Total courses: " + courses.size());
        r.add("ACTIVE enrollments: " + active);
        r.add("CANCELLED enrollments: " + cancelled);
        r.add("Distinct students with active enrollments: " + activeStudents.size());
        r.add("Seats used per course:");
        for (Course c : courses) {
            r.add("Course " + c.getId() + " | " + c.getTitle() + " | " + countActive(c.getId()) + " of " + c.getCapacity());
        }
        return r;
    }

    public void showReport() {
        for (String line : buildReport()) {
            System.out.println(line);
        }
    }

    public void saveReport() throws IOException {
        files.saveReport(buildReport());
    }
}
