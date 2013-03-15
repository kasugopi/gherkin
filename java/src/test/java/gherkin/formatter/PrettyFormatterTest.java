package gherkin.formatter;

import gherkin.formatter.model.*;
import org.junit.Test;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import gherkin.parser.Parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class PrettyFormatterTest {
    private static final List<Comment> NO_COMMENTS = emptyList();
    private static final List<Tag> NO_TAGS = Collections.emptyList();

    @Test
    public void prints_nice_colors() throws UnsupportedEncodingException, InterruptedException {
        PrettyFormatter f = new PrettyFormatter(System.out, false, false);
        f.scenario(new Scenario(NO_COMMENTS, NO_TAGS, "Scenario", "a scenario", "", 1, "a-scenario"));
        f.step(new Step(new ArrayList<Comment>(), "Given ", "I have 6 cukes", 1, null, null));
        Thread.sleep(1000);
        f.match(new Match(Arrays.asList(new Argument(7, "6")), "somewhere.brainfuck"));
        Thread.sleep(1000);
        f.result(new Result("failed", 55L, "Something\nbad\nhappened"));
    }

    @Test
    public void prints_table() throws UnsupportedEncodingException, InterruptedException {
        PrettyFormatter f = new PrettyFormatter(System.out, false, false);
        f.scenario(new Scenario(NO_COMMENTS, Collections.<Tag>emptyList(), "Scenario", "a scenario", "", 1, "a-scenario"));
        ArrayList<DataTableRow> rows = new ArrayList<DataTableRow>() {{
            add(new DataTableRow(NO_COMMENTS, asList("un", "deux"), 1, Row.DiffType.NONE));
            add(new DataTableRow(NO_COMMENTS, asList("one", "two"), 1, Row.DiffType.DELETE));
            add(new DataTableRow(NO_COMMENTS, asList("en", "to"), 1, Row.DiffType.INSERT));
        }};
        Step step = new Step(new ArrayList<Comment>(), "Given ", "I have 6 cukes", 1, rows, null);
        f.step(step);
        Thread.sleep(1000);
        f.match(new Match(Arrays.asList(new Argument(7, "6")), "somewhere.brainfuck"));
        Thread.sleep(1000);
        f.result(new Result("failed", 55L, "Something\nbad\nhappened"));
    }
    
    @Test
    public void shouldNotCloseProvidedStreamInDone() {
        PrintStream out = mock(PrintStream.class);
        Formatter formatter = new PrettyFormatter(out, true, true);
        formatter.done();
        verify(out, never()).close();
    }

    @Test
    public void shouldFlushAndCloseProvidedStreamInClose() {
        PrintStream out = mock(PrintStream.class);
        Formatter formatter = new PrettyFormatter(out, true, true);
        formatter.close();
        verify(out).flush();
        verify(out).close();
    }
    
    @Test
    public void shouldFormatAsDesigned() throws IOException {

        StringBuilder featureBuilder = new StringBuilder();
        featureBuilder.append("# language: pt\n");
        featureBuilder.append("Funcionalidade: formatar no Cucumber\n");
        featureBuilder.append("Cenário: O Cucumber consegue formatar uma tabela incompleta.\n");
        featureBuilder.append("Quando eu monto uma tabela como esta:\n");
        featureBuilder.append("\t|nome|valor|\n");
        featureBuilder.append("\t|a|b|\n");
        featureBuilder.append("Então deve formatar corretamente.\n");
        String feature = featureBuilder.toString();

        List<String> lines = doFormatter(feature);

        assertEquals("Formatter produces unexpected quantity of lines. ", 8, lines.size());

        assertEquals("# language: pt", lines.get(0));
        assertEquals("Funcionalidade: formatar no Cucumber", lines.get(1));
        assertEquals("", lines.get(2));
        assertEquals("  Cenário: O Cucumber consegue formatar uma tabela incompleta.", lines.get(3));
        assertEquals("    Quando eu monto uma tabela como esta:", lines.get(4));
        assertEquals("      | nome | valor |", lines.get(5));
        assertEquals("      | a    | b     |", lines.get(6));
        assertEquals("    Então deve formatar corretamente.", lines.get(7));

    }

    /**
     * @see https://github.com/cucumber/gherkin/issues/175
     */
    @Test
    public void issue175a() throws IOException {

        StringBuilder featureBuilder = new StringBuilder();
        featureBuilder.append("# language: pt\n");
        featureBuilder.append("Funcionalidade: formatar no Cucumber\n");
        featureBuilder.append("Cenário: O Cucumber consegue formatar uma tabela incompleta.\n");
        featureBuilder.append("Quando eu monto uma tabela como esta:\n");
        featureBuilder.append("\t|nome|valor|\n");
        featureBuilder.append("\t|a|\n"); // <--- here is different
        featureBuilder.append("Então deve formatar corretamente.\n");
        String feature = featureBuilder.toString();

        List<String> lines = doFormatter(feature);

        assertEquals("Formatter produces unexpected quantity of lines. ", 8, lines.size());

        assertEquals("      | a    |       |", lines.get(6));

    }

    @Test
    public void issue175b() throws IOException {

        StringBuilder featureBuilder = new StringBuilder();
        featureBuilder.append("# language: pt\n");
        featureBuilder.append("Funcionalidade: formatar no Cucumber\n");
        featureBuilder.append("Cenário: O Cucumber consegue formatar uma tabela incompleta.\n");
        featureBuilder.append("Quando eu monto uma tabela como esta:\n");
        featureBuilder.append("\t|nome|valor|\n");
        featureBuilder.append("\t|a|b|c|\n"); // <--- here is different
        featureBuilder.append("Então deve formatar corretamente.\n");
        String feature = featureBuilder.toString();

        List<String> lines = doFormatter(feature);

        assertEquals("Formatter produces unexpected quantity of lines. ", 8, lines.size());

        assertEquals("      | nome | valor |   |", lines.get(5));
        assertEquals("      | a    | b     | c |", lines.get(6));

    }

    /**
     * Execute a formatting feature for many different scenarios.
     * 
     * @param feature
     * @return
     * @throws IOException
     */
    private List<String> doFormatter(String feature) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        Formatter formatter = new PrettyFormatter(out, true, false);
        new Parser(formatter).parse(feature, "", 0);
        formatter.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));

        String line;
        List<String> lines = new ArrayList<String>();
        // int lineNumber = 0;

        while ((line = br.readLine()) != null) {
            // System.out.println(lineNumber+":"+line);
            // lineNumber++;
            lines.add(line);
        }

        return lines;

    }
    
}
