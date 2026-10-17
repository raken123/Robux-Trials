import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.File;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** encode <payload file>: prints the QR modules; decode <png...>: prints what each image decodes to. */
public class QrTool {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("encode")) {
            String text = Files.readString(new File(args[1]).toPath()).trim();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
            for (int y = 0; y < m.getHeight(); y++) {
                StringBuilder sb = new StringBuilder("QR:");
                for (int x = 0; x < m.getWidth(); x++) sb.append(m.get(x, y) ? '1' : '0');
                System.out.println(sb);
            }
        } else {
            for (int i = 1; i < args.length; i++) {
                var img = ImageIO.read(new File(args[i]));
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                var bmp = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
                try {
                    System.out.println("DECODED " + args[i] + " => " + new MultiFormatReader().decode(bmp, hints).getText());
                } catch (Exception e) {
                    System.out.println("FAILED " + args[i] + " => " + e);
                    System.exit(1);
                }
            }
        }
    }
}
