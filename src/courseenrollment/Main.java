package courseenrollment;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static CourseService service;
    private static final Admin admin = new Admin();

    public static void main(String[] args) {
        try {
            service = new CourseService(new FileManager("data"));
        } catch (IOException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Fix the file in the data folder and run again.");
            return;
        }
        try {
            boolean running = true;
            while (running) {
                System.out.println("\nMAIN MENU");
                System.out.println("1. Register student");
                System.out.println("2. Admin menu");
                System.out.println("3. Student menu");
                System.out.println("0. Exit");
                switch (readInt("Choice: ", 0, 3)) {
                    case 1 -> registerStudent();
                    case 2 -> showAdminMenu();
                    case 3 -> showStudentMenu();
                    case 0 -> running = false;
                }
            }
            System.out.println("Goodbye.");
        } catch (NoSuchElementException e) {
            System.out.println("\nInput ended. Goodbye.");
        }
    }

    // ---------- input helpers ----------

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(line);
                if (v >= min && v <= max) return v;
                System.out.println("Enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (FileManager.isValidText(line)) return line.trim();
            System.out.println("This field is required and cannot contain | or line breaks.");
        }
    }

    private static boolean askYes(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("y")) return true;
            if (line.equals("n")) return false;
            System.out.println("Enter y or n.");
        }
    }

    // ---------- menus ----------

    private static void registerStudent() {
        String name = readText("Name: ");
        String dept = readText("Department: ");
        try {
            Student s = service.registerStudent(name, dept);
            System.out.println("Registered. Your student ID is " + s.getId());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    public static void showAdminMenu() {
        User user = admin;
        user.showProfile();
        while (true) {
            System.out.println("\nADMIN MENU");
            System.out.println("1. Add course");
            System.out.println("2. View courses");
            System.out.println("3. View students");
            System.out.println("4. View enrollments");
            System.out.println("5. View reports");
            System.out.println("0. Back");
            switch (readInt("Choice: ", 0, 5)) {
                case 1 -> addCourse();
                case 2 -> viewCourses();
                case 3 -> service.printStudents();
                case 4 -> service.printEnrollments(null);
                case 5 -> viewReport();
                case 0 -> { return; }
            }
        }
    }

    public static void showStudentMenu() {
        Student student;
        while (true) {
            int id = readInt("Enter student ID (0 to go back): ", 0, Integer.MAX_VALUE);
            if (id == 0) return;
            student = service.findStudent(id);
            if (student != null) break;
            System.out.println("Student ID " + id + " not found.");
        }
        User user = student;
        user.showProfile();
        while (true) {
            System.out.println("\nSTUDENT MENU");
            System.out.println("1. View courses");
            System.out.println("2. Enroll in course");
            System.out.println("3. Cancel my enrollment");
            System.out.println("4. View my enrollment history");
            System.out.println("0. Back");
            switch (readInt("Choice: ", 0, 4)) {
                case 1 -> viewCourses();
                case 2 -> enroll(student);
                case 3 -> cancel(student);
                case 4 -> service.printEnrollments(student.getId());
                case 0 -> { return; }
            }
        }
    }

    // ---------- actions ----------

    private static void addCourse() {
        String title = readText("Course title: ");
        String trainer = readText("Trainer name: ");
        String[] batches = Course.getBatches();
        for (int i = 0; i < batches.length; i++) {
            System.out.println((i + 1) + ". " + batches[i]);
        }
        int batchChoice = readInt("Batch: ", 1, batches.length);
        int capacity = readInt("Capacity (1-" + Course.MAX_CAPACITY + "): ", 1, Course.MAX_CAPACITY);
        try {
            Course c = service.addCourse(title, trainer, batchChoice, capacity);
            System.out.println("Course " + c.getId() + " saved.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void viewCourses() {
        String[] batches = Course.getBatches();
        System.out.println("Filter by batch: 0. All");
        for (int i = 0; i < batches.length; i++) {
            System.out.println((i + 1) + ". " + batches[i]);
        }
        int choice = readInt("Choice: ", 0, batches.length);
        service.printCourses(choice == 0 ? null : batches[choice - 1]);
    }

    private static void enroll(Student student) {
        int courseId;
        while (true) {
            courseId = readInt("Enroll in course ID (0 to go back): ", 0, Integer.MAX_VALUE);
            if (courseId == 0) return;
            if (service.findCourse(courseId) != null) break;
            System.out.println("Course ID " + courseId + " not found.");
        }
        try {
            Enrollment e = service.enroll(student.getId(), courseId);
            Course c = service.findCourse(courseId);
            System.out.println("Enrollment " + e.getId() + " saved. Status: " + e.getStatus());
            System.out.println("Free seats in course " + courseId + ": "
                    + (c.getCapacity() - service.countActive(courseId)));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void cancel(Student student) {
        int id;
        while (true) {
            id = readInt("Cancel my enrollment ID (0 to go back): ", 0, Integer.MAX_VALUE);
            if (id == 0) return;
            if (service.findEnrollment(id) != null) break;
            System.out.println("Enrollment ID " + id + " not found.");
        }
        try {
            Enrollment e = service.cancelEnrollment(student.getId(), id);
            Course c = service.findCourse(e.getCourseId());
            System.out.println("Enrollment " + e.getId() + " saved. Status: " + e.getStatus());
            System.out.println("Free seats in course " + c.getId() + ": "
                    + (c.getCapacity() - service.countActive(c.getId())));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void viewReport() {
        service.showReport();
        if (askYes("Save report")) {
            try {
                service.saveReport();
                System.out.println("Report saved to data/report.txt");
            } catch (IOException e) {
                System.out.println("Could not save report: " + e.getMessage());
            }
        }
    }
}
