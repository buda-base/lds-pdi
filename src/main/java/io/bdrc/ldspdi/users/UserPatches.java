package io.bdrc.ldspdi.users;

public class UserPatches {

    public static String NL = System.lineSeparator();

    public static String getSetActivePatch(String userId, boolean active) {
        StringBuffer buff = new StringBuffer();
        buff.append("TX ." + NL);
        buff.append(" D <" + BudaUser.BDU_PFX + userId + "> <http://purl.bdrc.io/ontology/ext/user/isActive> \"" + Boolean.toString(!active) + "\" <" + BudaUser.PRIVATE_PFX + userId + ">");
        buff.append(NL + " A <" + BudaUser.BDU_PFX + userId + "> <http://purl.bdrc.io/ontology/ext/user/isActive> \"" + Boolean.toString(active) + "\" <" + BudaUser.PRIVATE_PFX + userId + ">");
        buff.append(NL + "TC ." + NL);
        return buff.toString();
    }

    public static void main(String[] args) {
        System.out.println(getSetActivePatch("U456", false));
    }

}
