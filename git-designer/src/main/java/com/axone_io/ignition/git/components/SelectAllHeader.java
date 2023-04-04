package com.axone_io.ignition.git.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class SelectAllHeader extends JCheckBox implements TableCellRenderer {
    private JTable table;
    private TableModel tableModel;
    private JTableHeader header;
    private TableColumnModel tcm;
    private int targetColumn;
    private int viewColumn;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public SelectAllHeader(JTable table, int targetColumn) {
        super();
        this.table = table;
        this.tableModel = table.getModel();
        if (tableModel.getColumnClass(targetColumn) != Boolean.class) {
            throw new IllegalArgumentException("Boolean column required.");
        }
        this.targetColumn = targetColumn;
        this.header = table.getTableHeader();
        setHorizontalAlignment(JCheckBox.CENTER);
        this.tcm = table.getColumnModel();
        this.applyUI();
        this.addItemListener(new ItemHandler());
        header.addMouseListener(new MouseHandler());
        setBorderPainted(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,new Color(-4143670)));
        tableModel.addTableModelListener(new ModelHandler());
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return this;
    }

    private class ItemHandler implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean state = e.getStateChange() == ItemEvent.SELECTED;
            setSelected(state);
            for (int r = 0; r < table.getRowCount(); r++) {
                table.setValueAt(state, r, viewColumn);
            }
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyUI();
    }

    private void applyUI() {
        this.setFont(UIManager.getFont("TableHeader.font"));
        this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        this.setBackground(UIManager.getColor("TableHeader.background"));
        this.setForeground(UIManager.getColor("TableHeader.foreground"));
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            viewColumn = header.columnAtPoint(e.getPoint());
            int modelColumn = tcm.getColumn(viewColumn).getModelIndex();
            if (modelColumn == targetColumn) {
                doClick();
                header.repaint();
            }
        }
    }

    private class ModelHandler implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            if (needsToggle()) {
                doClick();
                header.repaint();
            }
        }
    }

    // Return true if this toggle needs to match the model.
    private boolean needsToggle() {
        boolean allTrue = true;
        boolean allFalse = true;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            boolean b = (Boolean) tableModel.getValueAt(r, targetColumn);
            allTrue &= b;
            allFalse &= !b;
        }
        return allTrue && !isSelected() || allFalse && isSelected();
    }
}

