import dao.ResultDAO;

/**
 * Database-free check of the grading scale. Run with:
 *   javac -d out -cp "lib/*" $(find src test -name "*.java")
 *   java -cp out GradeCalculationTest
 */
public class GradeCalculationTest {
    public static void main(String[] args) {
        Object[][] cases = {
            {100.0, "A+"}, {90.0, "A+"}, {89.99, "A"}, {80.0, "A"}, {79.9, "B+"}, {70.0, "B+"},
            {69.9, "B"}, {60.0, "B"}, {59.9, "C"}, {50.0, "C"}, {49.9, "D"}, {40.0, "D"}, {39.9, "F"}, {0.0, "F"}
        };
        int failed = 0;
        for (Object[] c : cases) {
            String got = ResultDAO.calculateGrade((Double) c[0]);
            boolean ok = got.equals(c[1]);
            if (!ok) failed++;
            System.out.println((ok ? "PASS " : "FAIL ") + c[0] + "% -> " + got + " (expected " + c[1] + ")");
        }
        if (failed > 0) { System.out.println(failed + " test(s) failed"); System.exit(1); }
        System.out.println("All grade tests passed");
    }
}
