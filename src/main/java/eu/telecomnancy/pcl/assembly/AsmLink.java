package eu.telecomnancy.pcl.assembly;

import static eu.telecomnancy.pcl.Main.registerAllocator;

import eu.telecomnancy.pcl.assembly.register.RegisterAllocator;
import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.Map;
import java.util.Set;

public class AsmLink {
    private final AsmWriter writer;

    public AsmLink(AsmWriter writer) {
        this.writer = writer;
    }

    public void linkFrame(SymbolTable tds) {
        tds.enterScope();

        /* dyn link */
        writer.stw(RegisterAllocator.BASE_POINTER, "-(" + RegisterAllocator.STACK_POINTER + ")");

        /* static link */
        writer.stw(RegisterAllocator.RET, "-(" + RegisterAllocator.STACK_POINTER + ")");

        /* frame base */
        writer.ldw(RegisterAllocator.BASE_POINTER, RegisterAllocator.STACK_POINTER);

        /* local variables */
        Set<Map.Entry<String, Record>> records = tds.getCurrent().getRecords();
        int size = 0;

        for (Map.Entry<String, Record> entry : records) {
            Record rec = entry.getValue();
            /* TODO: owned */
            if (rec.getGenre() == Record.Genre.VARIABLE && (rec.getType() != ASTType.STRING)) {
                if (rec.getOffset() < 0) /* only local vars */
                    size -= rec.getSize();
            }
        }

        if (size != 0) {
            writer.adi(RegisterAllocator.STACK_POINTER, RegisterAllocator.STACK_POINTER, "#" + size);
        }
    }

    public void unlinkFrame(SymbolTable tds) {
        /* reset SP */
        writer.ldw(RegisterAllocator.STACK_POINTER, RegisterAllocator.BASE_POINTER);

        /* reset static link */
        writer.adq("2", RegisterAllocator.STACK_POINTER);

        /* reset dyn link */
        writer.ldw(RegisterAllocator.BASE_POINTER, "(" + RegisterAllocator.STACK_POINTER + ")+");

        tds.exitScope();
    }

}
