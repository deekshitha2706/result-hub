package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Base class for the "Add ..." pop-up forms: title, label/field rows, and
 * Cancel / Save buttons. Subclasses add rows and implement {@link #onSave()}.
 */
public abstract class FormDialog extends JDialog {
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JLabel status = Theme.label(" ", Theme.SMALL, Theme.DANGER);
    private final JButton save;
    private int row = 0;

    protected FormDialog(Window owner, String title, String subtitle, String saveText) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Theme.label(title, Theme.H2, Theme.TEXT));
        head.add(Box.createVerticalStrut(2));
        head.add(Theme.muted(subtitle));
        root.add(head, BorderLayout.NORTH);

        form.setOpaque(false);
        root.add(form, BorderLayout.CENTER);

        JButton cancel = Theme.button("Cancel", Theme.Kind.SECONDARY);
        cancel.addActionListener(e -> dispose());
        save = Theme.button(saveText);
        save.addActionListener(e -> onSave());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(status, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Adds "Label  [field]" as the next row; hint (optional) appears under the field. */
    protected <T extends JComponent> T addRow(String label, T field, String hint) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        form.add(Theme.label(label, Theme.BOLD, Theme.TEXT), c);

        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(5, 0, 5, 0);
        JComponent cell = field;
        if (hint != null) {
            JPanel p = new JPanel(new BorderLayout(0, 2));
            p.setOpaque(false);
            p.add(field, BorderLayout.CENTER);
            p.add(Theme.label(hint, Theme.SMALL, Theme.MUTED), BorderLayout.SOUTH);
            cell = p;
        }
        form.add(cell, c);
        return field;
    }

    protected <T extends JComponent> T addRow(String label, T field) { return addRow(label, field, null); }

    protected void showError(String message) { status.setForeground(Theme.DANGER); status.setText(message); }
    protected void showNote(String message) { status.setForeground(Theme.MUTED); status.setText(message); }
    protected void setSaving(boolean saving) { save.setEnabled(!saving); }

    /** Call after adding rows: sizes and centers the dialog. */
    protected void open() {
        pack();
        setMinimumSize(new Dimension(Math.max(getWidth(), 440), getHeight()));
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    protected abstract void onSave();
}
