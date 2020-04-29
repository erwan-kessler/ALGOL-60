package eu.telecomnancy.pcl.assembly;

import eu.telecomnancy.pcl.assembly.register.RegisterAllocator;

public class AsmWriter {
    private final StringBuffer code;
    private final StringBuffer heap;
    private int spos;
    private int tabs;


    public AsmWriter(StringBuffer code,StringBuffer heap) {
        this.code = code;
        this.heap=heap;
        spos = 0;
        tabs = 0;
        writeStatic("// STATIC ZONE");
    }

    public void tabsInc() {
        tabs++;
    }

    public void tabsDec() {
        tabs--;
    }

    public void write(String str) {
        for (int i = 0; i < tabs; ++i)
                code.append("  ");
        code.append(str).append("\n");
    }

    public void writeStatic(String str) {
        code.insert(spos, "  " + str + "\n");
        spos += str.length()+3;
    }
    public void writeHeap(String str){
        heap.append(str).append("\n");
    }

    public void adc(String R1, String R2, String R3) {
        write(String.format("adc %s, %s, %s", R1, R2, R3));
    }

    public void xor(String R1, String R2, String R3) {
        write(String.format("xor %s, %s, %s", R1, R2, R3));
    }

    public void div(String R1, String R2, String R3) {
        /* handle side effects */
        stw(R1, "-(" + RegisterAllocator.STACK_POINTER + ")");
        write(String.format("div %s, %s, %s", R1, R2, R3));
        ldw(R1, "(" + RegisterAllocator.STACK_POINTER + ")+");
    }

    public void mul(String R1, String R2, String R3) {
        write(String.format("mul %s, %s, %s", R1, R2, R3));
    }

    public void and(String R1, String R2, String R3) {
        write(String.format("and %s, %s, %s", R1, R2, R3));
    }

    public void or(String R1, String R2, String R3) {
        write(String.format("or %s, %s, %s", R1, R2, R3));
    }

    public void add(String R1, String R2, String R3) {
        write(String.format("add %s, %s, %s", R1, R2, R3));
    }

    public void sub(String R1, String R2, String R3) {
        write(String.format("sub %s, %s, %s", R1, R2, R3));
    }

    public void rlc(String R1, String R2) {
        write(String.format("rlc %s, %s", R1, R2));
    }

    public void rrc(String R1, String R2) {
        write(String.format("rrc %s, %s", R1, R2));
    }

    public void srl(String R1, String R2) {
        write(String.format("srl %s, %s", R1, R2));
    }

    public void sra(String R1, String R2) {
        write(String.format("sra %s, %s", R1, R2));
    }

    public void not(String R1, String R2) {
        write(String.format("not %s, %s", R1, R2));
    }

    public void sbb(String R1, String R2) {
        write(String.format("sbb %s, %s", R1, R2));
    }

    public void shl(String R1, String R2) {
        write(String.format("shl %s, %s", R1, R2));
    }

    public void neg(String R1, String R2) {
        write(String.format("neg %s, %s", R1, R2));
    }

    public void inp(String R1, String R2) {
        write(String.format("inp %s, %s", R1, R2));
    }

    public void out(String R1, String R2) {
        write(String.format("out %s, %s", R1, R2));
    }

    public void swb(String R1, String R2) {
        write(String.format("swb %s, %s", R1, R2));
    }

    public void rlb(String R1, String R2) {
        write(String.format("rlb %s, %s", R1, R2));
    }

    public void ani(String R1, String R2, String R3) {
        write(String.format("ani %s, %s, %s", R1, R2, R3));
    }

    public void ext(String R1, String R2) {
        write(String.format("ext %s, %s", R1, R2));
    }

    public void adi(String R1, String R2, String R3) {
        write(String.format("adi %s, %s, %s", R1, R2, R3));
    }

    public void cmp(String R1, String R2) {
        write(String.format("cmp %s, %s", R1, R2));
    }

    public void stb(String R1, String R2) {
        write(String.format("stb %s, %s", R1, R2));
    }

    public void stw(String R1, String R2) {
        write(String.format("stw %s, %s", R1, R2));
    }

    public void stl(String R1, String R2) {
        write(String.format("stl %s, %s", R1, R2));
    }

    public void ldb(String R1, String R2) {
        write(String.format("ldb %s, %s", R1, R2));
    }

    public void ldw(String R1, String R2) {
        write(String.format("ldw %s, %s", R1, R2));
    }

    public void ldl(String R1, String R2) {
        write(String.format("ldl %s, %s", R1, R2));
    }

    public void jmp(String R1) {
        write(String.format("jmp %s", R1));
    }

    public void jeq(String R1) {
        write(String.format("jeq %s", R1));
    }

    public void jne(String R1) {
        write(String.format("jne %s", R1));
    }

    public void jge(String R1) {
        write(String.format("jge %s", R1));
    }

    public void jle(String R1) {
        write(String.format("jle %s", R1));
    }

    public void jgt(String R1) {
        write(String.format("jgt %s", R1));
    }

    public void jlw(String R1) {
        write(String.format("jlw %s", R1));
    }

    public void jae(String R1) {
        write(String.format("jae %s", R1));
    }

    public void jcc(String R1) {
        write(String.format("jcc %s", R1));
    }

    public void jbe(String R1) {
        write(String.format("jbe %s", R1));
    }

    public void jab(String R1) {
        write(String.format("jab %s", R1));
    }

    public void jbl(String R1) {
        write(String.format("jbl %s", R1));
    }

    public void jcs(String R1) {
        write(String.format("jcs %s", R1));
    }

    public void jvs(String R1) {
        write(String.format("jvs %s", R1));
    }

    public void jvc(String R1) {
        write(String.format("jvc %s", R1));
    }

    public void jea(String R1) {
        write(String.format("jea %s", R1));
    }

    public void jsr(String R1) {
        write(String.format("jsr %s", R1));
    }

    public void trp(String R1) {
        write(String.format("trp %s", R1));
    }

    public void tst(String R1) {
        write(String.format("tst %s", R1));
    }

    public void tsr(String R1) {
        write(String.format("tsr %s", R1));
    }

    public void msr(String R1) {
        write(String.format("msr %s", R1));
    }

    public void mpc(String R1) {
        write(String.format("mpc %s", R1));
    }

    public void nop() {
        write("nop");
    }

    public void hlt() {
        write("hlt");
    }

    public void rts() {
        write("rts");
    }

    public void rti() {
        write("rti");
    }

    public void clc() {
        write("clc");
    }

    public void stc() {
        write("stc");
    }

    public void dsi() {
        write("dsi");
    }

    public void eni() {
        write("eni");
    }

    public void ldq(String R1, String R2) {
        write(String.format("ldq %s, %s", R1, R2));
    }

    public void adq(String R1, String R2) {
        write(String.format("adq %s, %s", R1, R2));
    }

    public void bmp(String R1) {
        write(String.format("bmp %s", R1));
    }

    public void beq(String R1) {
        write(String.format("beq %s", R1));
    }

    public void bne(String R1) {
        write(String.format("bne %s", R1));
    }

    public void bge(String R1) {
        write(String.format("bge %s", R1));
    }

    public void ble(String R1) {
        write(String.format("ble %s", R1));
    }

    public void bgt(String R1) {
        write(String.format("bgt %s", R1));
    }

    public void blw(String R1) {
        write(String.format("blw %s", R1));
    }

    public void bae(String R1) {
        write(String.format("bae %s", R1));
    }

    public void bcc(String R1) {
        write(String.format("bcc %s", R1));
    }

    public void bbe(String R1) {
        write(String.format("bbe %s", R1));
    }

    public void bab(String R1) {
        write(String.format("bab %s", R1));
    }

    public void bbl(String R1) {
        write(String.format("bbl %s", R1));
    }

    public void bcs(String R1) {
        write(String.format("bcs %s", R1));
    }

    public void bvs(String R1) {
        write(String.format("bvs %s", R1));
    }

    public void bvc(String R1) {
        write(String.format("bvc %s", R1));
    }

    public void label(String label) {
        write(label);
    }

    public void comment(String comment) {
        write(String.format("// %s", comment));
    }
}
