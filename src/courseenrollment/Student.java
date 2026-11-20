package courseenrollment;

public class Student extends User {
    private String department;

    public Student(int id, String name, String department) {
        super(id, name);
        setDepartment(department);
    }

    public String getDepartment() { return department; }

    public final void setDepartment(String department) {
        if (!FileManager.isValidText(department)) {
            throw new IllegalArgumentException("Department is required and cannot contain | or line breaks.");
        }
        this.department = department.trim();
    }

    @Override
    public void showProfile() {
        System.out.println("Student | ID: " + getId() + " | Name: " + getName()
                + " | Department: " + department);
    }
}
