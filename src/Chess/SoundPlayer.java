package Chess;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class SoundPlayer {
    public enum Effect {
        MOVE,
        CAPTURE,
        CHECK,
        GAME_OVER
    }

    private static final float SAMPLE_RATE = 44_100f;

    private SoundPlayer() {
    }

    public static void play(Effect effect, int volumePercent) {
        if (volumePercent <= 0) {
            return;
        }

        Thread soundThread = new Thread(
                () -> playEffect(effect, volumePercent),
                "chess-sound");
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private static void playEffect(Effect effect, int volumePercent) {
        int[] frequencies;
        int[] durations;

        switch (effect) {
            case CAPTURE:
                frequencies = new int[] { 330, 220 };
                durations = new int[] { 65, 90 };
                break;
            case CHECK:
                frequencies = new int[] { 660, 880 };
                durations = new int[] { 80, 110 };
                break;
            case GAME_OVER:
                frequencies = new int[] { 660, 520, 390, 260 };
                durations = new int[] { 130, 130, 150, 230 };
                break;
            case MOVE:
            default:
                frequencies = new int[] { 520 };
                durations = new int[] { 75 };
                break;
        }

        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,
                1,
                true,
                false);

        try {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            double volume = Math.min(100, volumePercent) / 100.0 * 0.28;

            for (int index = 0; index < frequencies.length; index++) {
                byte[] tone = createTone(
                        frequencies[index],
                        durations[index],
                        volume);
                line.write(tone, 0, tone.length);
            }

            line.drain();
            line.close();
        } catch (Exception exception) {
            // Sound is optional; unsupported audio devices should not stop play.
        }
    }

    private static byte[] createTone(
            int frequency,
            int durationMilliseconds,
            double volume) {

        int sampleCount = (int) (
                SAMPLE_RATE * durationMilliseconds / 1000.0);
        byte[] data = new byte[sampleCount * 2];

        for (int sample = 0; sample < sampleCount; sample++) {
            double time = sample / SAMPLE_RATE;
            double attack = Math.min(1.0, sample / (SAMPLE_RATE * 0.005));
            double release = Math.min(
                    1.0,
                    (sampleCount - sample) / (SAMPLE_RATE * 0.015));
            double envelope = Math.min(attack, release);
            short value = (short) (
                    Math.sin(2.0 * Math.PI * frequency * time)
                    * Short.MAX_VALUE
                    * volume
                    * envelope);

            data[sample * 2] = (byte) (value & 0xff);
            data[sample * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }

        return data;
    }
}
