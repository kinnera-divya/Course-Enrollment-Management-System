package courseenrollment;

public abstract class User {
    private final int id;
    private String name;

    protected User(int id, String name) {
        this.id = id;
        setName(name);
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public final void setName(String name) {
        if (!FileManager.isValidText(name)) {
            throw new IllegalArgumentException("Name is required and cannot contain | or line breaks.");
        }
        this.name = name.trim();
    }

    public abstract void showProfile();
}
