package vn.edu.it209;

public final class App {
    private App() {
    }

    public static String status() {
        return "Pipeline is healthy";
    }

    public static void main(String[] args) {
        System.out.println(status());
    }
}
