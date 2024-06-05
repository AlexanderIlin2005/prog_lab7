package server;

import common.lambdaworks.crypto.SCryptUtil;

public class ForSomeTest {
    public static void main(String[] args) {
        String originalPassword = "password";
        String generatedSecuredPasswordHash = SCryptUtil.scrypt(originalPassword, 32, 32, 32);
        System.out.println(generatedSecuredPasswordHash);

        boolean matched = SCryptUtil.check("password", generatedSecuredPasswordHash);
        System.out.println(matched);

        matched = SCryptUtil.check("passwordno", generatedSecuredPasswordHash);
        System.out.println(matched);
    }
}
