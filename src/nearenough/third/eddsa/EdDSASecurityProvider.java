/**
 * EdDSA-Java by str4d
 *
 * To the extent possible under law, the person who associated CC0 with
 * EdDSA-Java has waived all copyright and related or neighboring rights
 * to EdDSA-Java.
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <https://creativecommons.org/publicdomain/zero/1.0/>.
 *
 */
package nearenough.third.eddsa;

import nearenough.third.eddsa.spec.EdDSANamedCurveTable;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;

/**
 * A security {@link Provider} that can be registered via {@link Security#addProvider(Provider)}
 *
 * @author str4d
 */
public class EdDSASecurityProvider extends Provider {
    private static final long serialVersionUID = 1210027906682292307L;
    public static final String PROVIDER_NAME = "EdDSA";

    public EdDSASecurityProvider() {
        super(PROVIDER_NAME, 0.1 /* should match POM major.minor version */, "str4d " + PROVIDER_NAME + " security provider wrapper");

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                setup();
                return null;
            }
        });
    }

    protected void setup() {
        // see https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/HowToImplAProvider.html
        put("KeyPairGenerator." + EdDSAKey.KEY_ALGORITHM, "nearenough.third.eddsa.KeyPairGenerator");
        put("KeyFactory." + EdDSAKey.KEY_ALGORITHM, "nearenough.third.eddsa.KeyFactory");
        put("Signature." + EdDSANamedCurveTable.CURVE_ED25519_SHA512, "nearenough.third.eddsa.EdDSAEngine");
    }
}
