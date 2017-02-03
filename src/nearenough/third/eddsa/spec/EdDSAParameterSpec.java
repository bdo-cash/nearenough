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
package nearenough.third.eddsa.spec;

import nearenough.third.eddsa.math.Curve;
import nearenough.third.eddsa.math.GroupElement;
import nearenough.third.eddsa.math.ScalarOps;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Parameter specification for an EdDSA algorithm.
 * @author str4d
 *
 */
public class EdDSAParameterSpec implements AlgorithmParameterSpec, Serializable {
    private static final long serialVersionUID = 8274987108472012L;
    private final Curve curve;
    private final String hashAlgo;
    private final ScalarOps sc;
    private final GroupElement B;

    /**
     *  @throws IllegalArgumentException if hash algorithm is unsupported or length is wrong
     */
    public EdDSAParameterSpec(Curve curve, String hashAlgo,
            ScalarOps sc, GroupElement B) {
        try {
            MessageDigest hash = MessageDigest.getInstance(hashAlgo);
            // EdDSA hash function must produce 2b-bit output
            if (curve.getField().getb()/4 != hash.getDigestLength())
                throw new IllegalArgumentException("Hash output is not 2b-bit");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported hash algorithm");
        }

        this.curve = curve;
        this.hashAlgo = hashAlgo;
        this.sc = sc;
        this.B = B;
    }

    public Curve getCurve() {
        return curve;
    }

    public String getHashAlgorithm() {
        return hashAlgo;
    }

    public ScalarOps getScalarOps() {
        return sc;
    }

    /**
     *  @return the base (generator)
     */
    public GroupElement getB() {
        return B;
    }

    @Override
    public int hashCode() {
        return hashAlgo.hashCode() ^
               curve.hashCode() ^
               B.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EdDSAParameterSpec))
            return false;
        EdDSAParameterSpec s = (EdDSAParameterSpec) o;
        return hashAlgo.equals(s.getHashAlgorithm()) &&
               curve.equals(s.getCurve()) &&
               B.equals(s.getB());
    }
}
