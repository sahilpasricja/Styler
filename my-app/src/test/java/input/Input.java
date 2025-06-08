package input;


// This class is designed to have Checkstyle violations for testing

public class Input {

    // Should be annotated with @NonNull or @Nullable
    private String unannotatedField;

    // Should be annotated with @NonNull or @Nullable
    public Input(String unannotatedParam) {
        this.unannotatedField = unannotatedParam;
    }

    // Should have return type annotated
    public String getUnannotatedValue() {
        // Should have local variable annotated
        String unannotatedLocalVar = "hello";
        return unannotatedLocalVar;
    }

    // Should have parameter annotated
    public void processData(String data) {
        System.out.println(data);
    }
}