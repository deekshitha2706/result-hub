package ui;

import dao.CourseDAO;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import model.Course;

public class CoursesPanel extends ListPage {
    public CoursesPanel() {
        super("Courses", "Subjects that results can be recorded for.", "+ Add Course",
                "ID", "Course Code", "Course Name", "Credits", "Semester");
    }

    @Override
    protected List<Object[]> fetchRows() {
        List<Object[]> rows = new ArrayList<>();
        for (Course c : new CourseDAO().getAllCourses()) {
            rows.add(new Object[]{c.getCourseId(), c.getCourseCode(), c.getCourseName(), c.getCredits(), c.getSemester()});
        }
        return rows;
    }

    @Override
    protected void onAdd() {
        Window w = SwingUtilities.getWindowAncestor(this);
        new AddCourseDialog(w, this::refresh);
    }
}
