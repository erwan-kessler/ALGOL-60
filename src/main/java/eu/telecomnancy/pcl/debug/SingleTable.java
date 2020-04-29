package eu.telecomnancy.pcl.debug;

import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.Records.*;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

public class SingleTable extends JInternalFrame {
    static int openFrameCount = 0;
    static int yOffset = 450;
    private List<Integer> index;
    SymbolTable.Scope scope;
    ArrayList<SingleTable> children=new ArrayList<>();
    public SingleTable(){
        this.dispose();
    }
    public SingleTable(List<Integer> index, SymbolTable.Scope scope) {
        super(index + " index: " + index.size() + " (" + scope.getScopeType() + "), Frame #" + (++openFrameCount),
                true, //resizable
                false, //closable
                false, //maximizable
                false);//iconifiable

        this.scope = scope;
        this.index = index;
        String[] columns_variable = new String[]{
                "Identifier", "Type", "Static", "Offset","Size"
        };
        Object[][] data_variable = new Object[scope.getRecords().size()][columns_variable.length];
        String[] columns_array = new String[]{
                "Identifier", "Type", "Dimension", "Static"
        };
        Object[][] data_array = new Object[scope.getRecords().size()][columns_array.length];
        String[] columns_label = new String[]{
                "Identifier", "Type", "Offset","Size"
        };
        Object[][] data_label = new Object[scope.getRecords().size()][columns_label.length];
        String[] columns_procedure = new String[]{
                "Identifier", "Type", "Parameters", "Values","Size"
        };
        Object[][] data_procedure = new Object[scope.getRecords().size()][columns_procedure.length];
        String[] columns_switch = new String[]{
                "Identifier", "Type", "Labels", "Offset","Size"
        };
        Object[][] data_switch = new Object[scope.getRecords().size()][columns_switch.length];
        int index_array = 0;
        int index_var = 0;
        int index_proc = 0;
        int index_label = 0;
        int index_unknown = 0;
        for (Entry<String, Record> record : scope.getRecords()) {
            Record temp = record.getValue();
            if (temp instanceof ArrayRecord) {
                ArrayRecord temp_array = (ArrayRecord) temp;
                data_array[index_array] = new Object[]{temp_array.getId(), temp_array.getType(), temp_array.getDimension(), temp_array.getOwn()};
                index_array++;
            } else if (temp instanceof VariableRecord) {
                VariableRecord temp_var = (VariableRecord) temp;
                data_variable[index_var] = new Object[]{temp_var.getId(), temp_var.getType(), temp_var.getOwn(),temp_var.getOffset(),temp_var.getSize()};
                index_var++;
            } else if (temp instanceof ProcedureRecord) {
                ProcedureRecord temp_proc = (ProcedureRecord) temp;
                data_procedure[index_proc] = new Object[]{temp_proc.getId(), temp_proc.getType(), temp_proc.getParameters().toString(), temp_proc.getValues().toString(),temp_proc.getSize()};
                index_proc++;
            } else if (temp instanceof SwitchRecord) {
                SwitchRecord temp_switch = (SwitchRecord) temp;
                data_switch[index_unknown] = new Object[]{temp_switch.getId(), temp_switch.getType(), temp_switch.getSwitchList(),temp_switch.getOffset(),temp_switch.getSize()};
                index_unknown++;
            } else if (temp instanceof LabelRecord) {
                LabelRecord temp_label = (LabelRecord) temp;
                data_label[index_label] = new Object[]{temp_label.getId(), temp_label.getType(),temp_label.getOffset(),temp_label.getSize()};
                index_label++;
            } else {
                System.out.println("ERROR");
            }
        }
        //actual data for the table in a 2d array
        //create table with data
        data_array = Arrays.copyOf(data_array, index_array);
        data_procedure = Arrays.copyOf(data_procedure, index_proc);
        data_switch = Arrays.copyOf(data_switch, index_unknown);
        data_variable = Arrays.copyOf(data_variable, index_var);
        data_label = Arrays.copyOf(data_label, index_label);

        JTable table_variable = new JTable(data_variable, columns_variable);
        JTable table_procedure = new JTable(data_procedure, columns_procedure);
        JTable table_label = new JTable(data_label, columns_label);
        JTable table_array = new JTable(data_array, columns_array);
        JTable table_switch = new JTable(data_switch, columns_switch);

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));
        table_variable.setPreferredScrollableViewportSize(table_variable.getPreferredSize());
        table_procedure.setPreferredScrollableViewportSize(table_procedure.getPreferredSize());
        table_label.setPreferredScrollableViewportSize(table_label.getPreferredSize());
        table_array.setPreferredScrollableViewportSize(table_array.getPreferredSize());
        table_switch.setPreferredScrollableViewportSize(table_switch.getPreferredSize());

        tablePanel.add(new JLabel("Variable"));
        tablePanel.add(new JScrollPane(table_variable));
        tablePanel.add(new JLabel("Array"));
        tablePanel.add(new JScrollPane(table_array));
        tablePanel.add(new JLabel("Procedure"));
        tablePanel.add(new JScrollPane(table_procedure));
        tablePanel.add(new JLabel("Switch"));
        tablePanel.add(new JScrollPane(table_switch));
        tablePanel.add(new JLabel("Label"));
        tablePanel.add(new JScrollPane(table_label));


        this.add(new JScrollPane(tablePanel));
        setSize(400, 400);
        int level = index.size();
        setLocation(50, yOffset * (level - 1));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

    }
    public void addChildren(SingleTable singleTable){
        children.add(singleTable);
    }
    public List<Integer> getIndex() {
        return index;
    }

    public ArrayList<SingleTable> getChildren() {
        return children;
    }

    public SymbolTable.Scope getScope() {
        return scope;
    }
    private Integer calculateSize(SymbolTable.Scope scope){
        int size=0;
        if (scope.getChildren().isEmpty()){
            return 450;
        }
        for (SymbolTable.Scope children:scope.getChildren()){
            size+=calculateSize(children);
        }
        return size;
    }

    public void relocate(int[] maxChildren) {
        int currentOffset = 0;
        for (int i=1;i<index.size();i++) {
            currentOffset += (index.get(i))*maxChildren[i-1];
        }
        int off=1;
        for (int i=index.size();i<maxChildren.length;i++){
            off*=maxChildren[i-1];
        }
        currentOffset+=off;
        setLocation(currentOffset*225+50, getY());
    }
}
