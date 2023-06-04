package io.jenkins.pluginhealth.scoring.probes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ThirdPartyRepositoryDetectionProbeTest extends AbstractProbeTest<ThirdPartyRepositoryDetectionProbe> {
    @Override
    ThirdPartyRepositoryDetectionProbe getSpy() {
        return spy(ThirdPartyRepositoryDetectionProbe.class);
    }

    static Stream<Arguments> pomPathAndProbeSuccessTestParameters() {
        return Stream.of(
            arguments(
                Paths.get("src","test","resources","pom-test-only-correct-path"),
                Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""))
            ),
            arguments(
                Paths.get("src","test","resources","pom-test-no-repository-tag"),
                Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""))
            )
        );
    }
    static Stream<Arguments> pomPathAndProbeFailureTestParameters() {
        return Stream.of(
            arguments(
                Paths.get("src","test","resources","pom-test-both-paths"),
                Map.of(SCMLinkValidationProbe.KEY, ProbeResult.success(SCMLinkValidationProbe.KEY, ""))
            ),
            arguments(
                Paths.get("src","test","resources","pom-test-only-incorrect-path"),
            )
        );
    }
    @Test
    void shouldBeExecutedAfterSCMLinkValidationProbeProbe() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        final ThirdPartyRepositoryDetectionProbe probe = getSpy();

        when(plugin.getName()).thenReturn("foo-bar");

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "status")
            .isEqualTo(ProbeResult.error(ThirdPartyRepositoryDetectionProbe.KEY, ""));
        verify(probe, never()).doApply(plugin, ctx);
    }

    @Test
    void shouldPassIfNoThirdPartyRepositoriesDetected() {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);

        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        when(ctx.getScmRepository()).thenReturn(Path.of(absolutePath));

        final ThirdPartyRepositoryDetectionProbe probe = getSpy();
        when(plugin.getDetails()).thenReturn(details);

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.success(ThirdPartyRepositoryDetectionProbe.KEY, "The plugin has no third party repositories"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }

    @ParameterizedTest
    @MethodSource("pomPathAndProbeFailureTestParameters")
    void shouldFailIfThirdPartRepositoriesDetected(Path resourceDirectory, String[] probeResultRequirement, Map<String, ProbeResult> details) {
        final Plugin plugin = mock(Plugin.class);
        final ProbeContext ctx = mock(ProbeContext.class);
        when(plugin.getDetails()).thenReturn(details);

        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        when(ctx.getScmRepository()).thenReturn(Path.of(absolutePath));

        final ThirdPartyRepositoryDetectionProbe probe = getSpy();

        assertThat(probe.apply(plugin, ctx))
            .usingRecursiveComparison()
            .comparingOnlyFields("id", "message", "status")
            .isEqualTo(ProbeResult.failure(ThirdPartyRepositoryDetectionProbe.KEY, "Third party repositories detected in the plugin"));
        verify(probe).doApply(any(Plugin.class), any(ProbeContext.class));
    }


}
