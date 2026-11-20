package courseenrollment;

public class Enrollment {
    private final int id;
    private final int studentId;
    private final int courseId;
    private EnrollmentStatus status;

    public Enrollment(int id, int studentId, int courseId, EnrollmentStatus status) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = status;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getCourseId() { return courseId; }
    public EnrollmentStatus getStatus() { return status; }

    /** ACTIVE -> CANCELLED. Cannot be repeated. */
    public void cancel() {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Enrollment is already cancelled.");
        }
        status = EnrollmentStatus.CANCELLED;
    }

    /** Used only to roll back when saving the cancellation fails. */
    void undoCancel() { status = EnrollmentStatus.ACTIVE; }
}
