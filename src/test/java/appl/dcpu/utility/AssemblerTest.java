package appl.dcpu.utility;

import org.junit.Test;

import appl.dcpu.utility.Assembler.AssemblyResult;

public class AssemblerTest {

	private static final String PROGRAM1 = "; Try some basic stuff\n" +
"              SET A, 0x30              ; 7c01 0030\n" +
"              SEN [0x1000], 0x20       ; 7de1 1000 0020\n" +
"              SUB A, [0x1000]          ; 7803 1000\n" +
"              IFN A, 0x10              ; c00d \n" +
"                 SET PC,          ; 7dc1 001a [*]\n" +
"              \n" +
"; Do a loopy thing\n" +
"              SET I, 10                ; a861\n" +
"              SET A, 0x2000            ; 7c01 2000\n" +
":loop         SET [0x2000+I], [A]      ; 2161 2000\n" +
"              SUB I, 1                 ; 8463\n" +
"              IFN I, 0                 ; 806d\n" +
"                 SET PC, loop          ; 7dc1 000d [*]\n" +
"\n" +
"; Call a subroutine\n" +
"              SET X, 0x4               ; 9031\n" +
"              JSR testsub              ; 7c10 0018 [*]\n" +
"              SET PC, crash            ; 7dc1 001a [*]\n" +
"\n" +
":testsub      SHL X, 4                 ; 9037\n" +
"              SET PC, POP              ; 61c1\n" +
"                \n" +
"; Hang forever. X should now be 0x40 if everything went right.\n" +
":crash        SET PC, crash            ; 7dc1 001a [*]\n" +
":stuff		  dat \"text\",0x44,crash\n" +
"; [*]: Note that these can be one word shorter and one cycle faster by using the short form (0x00-0x1f) of literals,\n" +
";      but my assembler doesn't support short form labels yet. ";

	@Test
	public void runAssembler() {
		Assembler asm = new Assembler(PROGRAM1);
		AssemblyResult code = asm.assemble();
		System.out.println(code.hexResult);
		System.out.println(code.listing);
	}
}
