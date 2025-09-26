package work.lclpnet.gaco.ds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GreedyMeshingTest {

    @Test
    void generateBoxes_2d() {
        var piece = """
                0000000000██000
                00000000███████
                00██0000███████
                ███████████████
                00█████████████
                00█████████████
                ███████████████
                00000000███████
                0000000000000██""";

        int width = width(piece), height = height(piece);
        var struct = new String2dVoxelView(piece, width, height);
        var meshing = new GreedyMeshing(width, height, 1, struct);

        var boxes = meshing.generateBoxes();

        String expected = """
                0000000000aa000
                00000000bbaaccc
                00dd0000bbaaccc
                eeddffffbbaaccc
                00ddffffbbaaccc
                00ddffffbbaaccc
                ggddffffbbaaccc
                00000000bbaaccc
                0000000000000hh""";

        String actual = String2dVoxelView.genString(height, width, boxes);

        assertEquals(expected, actual);
    }

    @Test
    void generateBoxes_3d() {
        var y0 = """
                000000000000000
                00000000███████
                00000000███████
                00█████████████
                00█████████████
                00█████████████
                00█████████████
                00000000███████
                000000000000000""";
        var y1 = """
                0000000000██000
                00000000███████
                00██0000███████
                ███████████████
                00█████████████
                00█████████████
                ███████████████
                00000000███████
                0000000000000██""";
        var y2 = """
                000000000000000
                00000000███████
                00000000███████
                00000000███████
                00000000███████
                00000000███████
                00000000███████
                00000000███████
                000000000000000""";

        int width = width(y1), height = 3, length = height(y1);

        assertEquals(width, width(y0));
        assertEquals(width, width(y2));
        assertEquals(length, height(y0));
        assertEquals(length, height(y2));

        var struct = new String3dVoxelView(new String[] { y0, y1, y2 }, width, height, length);
        var meshing = new GreedyMeshing(width, height, length, struct);

        var boxes = meshing.generateBoxes();

        String[] expected = new String[] {
                """
                  000000000000000
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00bbbbbbaaaaaaa
                  00bbbbbbaaaaaaa
                  00bbbbbbaaaaaaa
                  00bbbbbbaaaaaaa
                  00000000aaaaaaa
                  000000000000000""",
                """
                  0000000000cc000
                  00000000aaaaaaa
                  00dd0000aaaaaaa
                  eebbbbbbaaaaaaa
                  00bbbbbbaaaaaaa
                  00bbbbbbaaaaaaa
                  ffbbbbbbaaaaaaa
                  00000000aaaaaaa
                  0000000000000gg""",
                """
                  000000000000000
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00000000aaaaaaa
                  00000000aaaaaaa
                  000000000000000"""};

        String[] actual = String3dVoxelView.genStrings(height, width, length, boxes);

        assertArrayEquals(expected, actual);
    }

    private static int width(String piece) {
        return piece.split("\n")[0].length();
    }

    private static int height(String piece) {
        return piece.split("\n").length;
    }
}