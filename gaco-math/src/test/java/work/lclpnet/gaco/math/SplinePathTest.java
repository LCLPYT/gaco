package work.lclpnet.gaco.math;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static work.lclpnet.gaco.math.SplinePath.findArcLengthSection;

class SplinePathTest {

    private static final double EPSILON = 1e-6;
    private static final Logger logger = LoggerFactory.getLogger(SplinePathTest.class);

    private SplinePath straightPath;
    private SplinePath curvedPath;

    @BeforeEach
    void setUp() {
        List<Vec3d> straightPoints = List.of(
                new Vec3d(0, 0, 0),
                new Vec3d(10, 0, 0),
                new Vec3d(20, 0, 0)
        );

        straightPath = new SplinePath(straightPoints);

        List<Vec3d> curvedPoints = List.of(
                new Vec3d(0, 0, 0),
                new Vec3d(10, 10, 0),
                new Vec3d(20, 0, 0)
        );

        curvedPath = new SplinePath(curvedPoints);
    }

    @Test
    @DisplayName("Constructor should throw when less than 2 keypoints are provided")
    void constructor_throwsForTooFewKeypoints() {
        assertThrows(IllegalArgumentException.class, () -> new SplinePath(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new SplinePath(List.of(new Vec3d(0, 0, 0))));
    }

    @Test
    @DisplayName("Constructor should succeed with 2 or more keypoints")
    void constructor_succeedsWithSufficientKeypoints() {
        assertDoesNotThrow(() -> new SplinePath(List.of(new Vec3d(0, 0, 0), new Vec3d(1, 1, 1))));
        assertDoesNotThrow(() -> new SplinePath(List.of(new Vec3d(0, 0, 0), new Vec3d(1, 1, 1), new Vec3d(2, 2, 2))));
    }

    @Test
    @DisplayName("getKeypoints should return the original keypoints")
    void getKeypoints_returnsOriginalPoints() {
        List<Vec3d> points = List.of(new Vec3d(1, 2, 3), new Vec3d(4, 5, 6));
        SplinePath path = new SplinePath(points);
        assertEquals(points, path.getKeypoints());
    }

    @Test
    @DisplayName("sampleLinear should return start point for t <= 0")
    void samplePositionLinear_returnsStartForTZeroOrLess() {
        assertEquals(new Vec3d(0, 0, 0), straightPath.samplePositionLinear(0.0));
        assertEquals(new Vec3d(0, 0, 0), straightPath.samplePositionLinear(-1.0));
    }

    @Test
    @DisplayName("sampleLinear should return end point for t >= 1")
    void samplePositionLinear_returnsEndForTOneOrMore() {
        assertEquals(new Vec3d(20, 0, 0), straightPath.samplePositionLinear(1.0));
        assertEquals(new Vec3d(20, 0, 0), straightPath.samplePositionLinear(2.0));
    }

    @Test
    @DisplayName("sampleLinear should correctly interpolate along a straight path")
    void samplePositionLinear_interpolatesStraightPath() {
        assertVectorEquals(new Vec3d(5, 0, 0), straightPath.samplePositionLinear(0.25));
        assertVectorEquals(new Vec3d(10, 0, 0), straightPath.samplePositionLinear(0.5));
        assertVectorEquals(new Vec3d(15, 0, 0), straightPath.samplePositionLinear(0.75));
    }

    @Test
    @DisplayName("sampleLinear should interpolate through keypoints on a curved path")
    void samplePositionLinear_interpolatesCurvedPathKeypoints() {
        assertVectorEquals(new Vec3d(0, 0, 0), curvedPath.samplePositionLinear(0.0));
        assertVectorEquals(new Vec3d(10, 10, 0), curvedPath.samplePositionLinear(0.5));
        assertVectorEquals(new Vec3d(20, 0, 0), curvedPath.samplePositionLinear(1.0));
    }

    @Test
    @DisplayName("sampleDirectionLinear should be constant for a straight path")
    void sampleFirstDerivativeLinear_isConstantForStraightPath() {
        Vec3d expectedDirection = new Vec3d(20, 0, 0); // (10-0)*2, (20-10)*2
        assertVectorEquals(expectedDirection, straightPath.sampleFirstDerivativeLinear(0.25));
        assertVectorEquals(expectedDirection, straightPath.sampleFirstDerivativeLinear(0.5));
        assertVectorEquals(expectedDirection, straightPath.sampleFirstDerivativeLinear(0.75));
    }

    @Test
    @DisplayName("sampleDirectionLinear should handle boundaries")
    void sampleFirstDerivativeLinear_handlesBoundaries() {
        assertNotNull(straightPath.sampleFirstDerivativeLinear(0.0));
        assertNotNull(straightPath.sampleFirstDerivativeLinear(1.0));
        assertTrue(straightPath.sampleFirstDerivativeLinear(0.0).dotProduct(new Vec3d(1,0,0)) > 0);
        assertTrue(straightPath.sampleFirstDerivativeLinear(1.0).dotProduct(new Vec3d(1,0,0)) > 0);
    }

    @Test
    @DisplayName("sampleCurvatureLinear should be zero for a straight path")
    void sampleSecondDerivativeLinear_isZeroForStraightPath() {
        assertVectorEquals(new Vec3d(0, 0, 0), straightPath.sampleSecondDerivativeLinear(0.0));
        assertVectorEquals(new Vec3d(0, 0, 0), straightPath.sampleSecondDerivativeLinear(0.5));
        assertVectorEquals(new Vec3d(0, 0, 0), straightPath.sampleSecondDerivativeLinear(1.0));
    }

    @Test
    @DisplayName("sample should return start point for s = 0")
    void samplePosition_returnsStartForSZero() {
        assertVectorEquals(new Vec3d(0, 0, 0), straightPath.samplePosition(0.0));
        assertVectorEquals(new Vec3d(0, 0, 0), curvedPath.samplePosition(0.0));
    }

    @Test
    @DisplayName("sample should return end point for s = 1")
    void samplePosition_returnsEndForSOne() {
        assertVectorEquals(new Vec3d(20, 0, 0), straightPath.samplePosition(1.0));
        assertVectorEquals(new Vec3d(20, 0, 0), curvedPath.samplePosition(1.0));
    }

    @Test
    @DisplayName("sample at s=0.5 on a straight path should be the midpoint")
    void samplePosition_isMidpointForStraightPath() {
        assertVectorEquals(new Vec3d(10, 0, 0), straightPath.samplePosition(0.5));
    }

    @Test
    @DisplayName("getLinearProgress and getNormalizedProgress should be inverse operations")
    void progressConversions_areInverse() {
        assertEquals(0.25, straightPath.getLinearProgress(straightPath.getProgress(0.25)), EPSILON);
        assertEquals(0.5, curvedPath.getLinearProgress(curvedPath.getProgress(0.5)), EPSILON);
        assertEquals(0.75, straightPath.getProgress(straightPath.getLinearProgress(0.75)), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.25, 0.5, 0.75, 1.0})
    @DisplayName("getProgress should find the correct progress for points on the path")
    void getProgress_findsCorrectNormalizedProgressForOnPathPoints(double t) {
        Vec3d pointOnPath = straightPath.samplePositionLinear(t);
        double expectedS = straightPath.getProgress(t);
        assertEquals(expectedS, straightPath.getProgress(pointOnPath), 1e-4);
    }

    @Test
    @DisplayName("getProgress should find the closest point for off-path queries")
    void getProgress_findsClosestPointForOffPathQueries() {
        Vec3d queryPos = new Vec3d(10, 5, 0); // Above the midpoint of the straight path
        double progress = straightPath.getProgress(queryPos);
        Vec3d closestPoint = straightPath.samplePosition(progress);
        assertVectorEquals(new Vec3d(10, 0, 0), closestPoint);
    }

    @Test
    @DisplayName("findArcLengthSection should find correct index")
    void findArcLengthSection_findsCorrectIndex() {
        double[] arcLength = {0.0, 1.0, 2.5, 4.0, 5.0};
        assertEquals(0, findArcLengthSection(arcLength, 0.0));
        assertEquals(0, findArcLengthSection(arcLength, 0.5));
        assertEquals(1, findArcLengthSection(arcLength, 1.0));
        assertEquals(1, findArcLengthSection(arcLength, 2.4));
        assertEquals(2, findArcLengthSection(arcLength, 2.5));
        assertEquals(3, findArcLengthSection(arcLength, 4.9));
        assertEquals(4, findArcLengthSection(arcLength, 5.0));
        assertEquals(4, findArcLengthSection(arcLength, 100.0));
        assertEquals(0, findArcLengthSection(arcLength, -10.0));
    }

    @Test
    @DisplayName("sampleArcLength should interpolate correctly")
    void sampleArcLength_interpolatesCorrectly() {
        assertEquals(0.0, straightPath.sampleArcLength(0.0), EPSILON);
        assertEquals(20.0, straightPath.sampleArcLength(1.0), EPSILON);
        assertEquals(10.0, straightPath.sampleArcLength(0.5), EPSILON);
    }

    @Test
    @DisplayName("estimateSegmentProgress should find the closest segment")
    void estimateSegmentProgress_findsClosestSegment() {
        Vec3d queryAtStart = new Vec3d(0, 1, 0);
        assertEquals(0.0, straightPath.estimateSegmentProgress(queryAtStart), EPSILON);

        // may fail depending on arclength sampling rate
        Vec3d queryAtMidpoint = new Vec3d(10, 5, 0);
        assertEquals(59/119d, straightPath.estimateSegmentProgress(queryAtMidpoint), EPSILON);

        Vec3d queryAtEnd = new Vec3d(20, -1, 0);
        assertEquals(1.0, straightPath.estimateSegmentProgress(queryAtEnd), EPSILON);
    }

    private static void assertVectorEquals(Vec3d expected, Vec3d actual) {
        assertEquals(expected.getX(), actual.getX(), EPSILON);
        assertEquals(expected.getY(), actual.getY(), EPSILON);
        assertEquals(expected.getZ(), actual.getZ(), EPSILON);
    }

    @Test
    void getLinearProgress_closeToStart_refinementWorks() {
        var json = new Gson().fromJson("""
                [
                        [2.862500011920929, 64.0, 0.525104285429158],
                        [21.30807830987182, 64.0, 0.8100902895445458],
                        [26.38299919662967, 64.0, 6.742668223692805],
                        [20.58800393941121, 64.0, 11.654621180307483],
                        [2.42490719892729, 64.0, 11.559314753032305],
                        [-7.622902230195006, 64.0, 19.161305835822073],
                        [-15.943090657090806, 64.0, 25.28677784749734],
                        [-24.95331600887882, 64.0, 19.665954687889784],
                        [-29.04549949142559, 64.0, 11.031239358749549],
                        [-38.09320252419119, 64.0, 10.2947321347738],
                        [-44.93673532468377, 64.0, 7.358275543071303],
                        [-33.037781328603245, 64.0, 0.27816089988028253],
                        [-28.288216638166727, 64.0, -7.3212209917609075],
                        [-23.115713866977508, 64.0, -27.590364129618],
                        [-15.300000011920929, 64.0, -32.69999998807907],
                        [-10.47913290020666, 64.0, -24.41417283755979],
                        [-10.457574674336943, 64.0, -8.124040251070861],
                        [-8.181050815450543, 64.0, -2.1569848998658925]
                      ]
                """, JsonArray.class);

        var path = SplinePath.CODEC.decode(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> logger.error("Failed to decode spline path from json: {}", err))
                .map(Pair::getFirst)
                .orElseThrow();

        var queryPos = new Vec3d(2.9702233002807463, 64.0, 0.43919395937908695);

        double linear = path.getLinearProgress(queryPos);

        assertEquals(0, linear, 1e-3);
    }
}