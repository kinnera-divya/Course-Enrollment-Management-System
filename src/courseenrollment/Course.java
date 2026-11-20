package courseenrollment;

public class Course {
    private static final String[] BATCHES = {"Morning", "Afternoon", "Evening"};
    public static final int MAX_CAPACITY = 1000;

    private final int id;
    private final int capacity;
    private final String title;
    private final String trainer;
    private final String batch;

    public Course(int id, String title, String trainer, String batch, int capacity) {
        if (!FileManager.isValidText(title)) {
            throw new IllegalArgumentException("Course title is required and cannot contain | or line breaks.");
        }
        if (!FileManager.isValidText(trainer)) {
            throw new IllegalArgumentException("Trainer name is required and cannot contain | or line breaks.");
        }
        if (!isValidBatch(batch)) {
            throw new IllegalArgumentException("Batch must be Morning, Afternoon or Evening.");
        }
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_CAPACITY + ".");
        }
        this.id = id;
        this.title = title.trim();
        this.trainer = trainer.trim();
        this.batch = batch;
        this.capacity = capacity;
    }

    /** Returns a copy so outside code cannot change the batch choices. */
    public static String[] getBatches() { return BATCHES.clone(); }

    public static boolean isValidBatch(String batch) {
        for (String b : BATCHES) {
            if (b.equals(batch)) return true;
        }
        return false;
    }

    public int getId() { return id; }
    public int getCapacity() { return capacity; }
    public String getTitle() { return title; }
    public String getTrainer() { return trainer; }
    public String getBatch() { return batch; }

    public void showDetails(int activeCount) {
        System.out.println(id + " | " + title + " | " + trainer + " | " + batch
                + " | Capacity: " + capacity + " | Active: " + activeCount
                + " | Free seats: " + (capacity - activeCount));
    }
}
