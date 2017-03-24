import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Nokosi on 23/03/2017.
 */

public class SDES {
    private boolean[] master_key;
    private boolean [][][] s_box1 = new boolean [4][4][2];
    private boolean [][][] s_box2 = new boolean [4][4][2];

    public SDES(String _key){
        master_key = new boolean[10];

        int index = 0;
        for (char ch: _key.toCharArray()){
            if(Character.getNumericValue(ch) == 1) {
                master_key[index] = true;
            } else {
                master_key[index] = false;
            }
            index++;
        }
    }


    //Génération des clés

    //P10 (k1, k2, k3, k4, k5, k6, k7, k8, k9, k10 ) = (k3, k5, k2, k7, k4, k10, k1, k9, k8, k6)
    private boolean[] p10 (boolean[] key){
        boolean[] permut = new boolean[10];
        permut[0] = key[2];
        permut[1] = key[4];
        permut[2] = key[1];
        permut[3] = key[6];
        permut[4] = key[3];
        permut[5] = key[9];
        permut[6] = key[0];
        permut[7] = key[8];
        permut[8] = key[7];
        permut[9] = key[5];
        return permut;
    }

    //P8(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10) = (k6, k3, k7, k4, k8, k5, k10, k9)
    private boolean[] p8 (boolean[] key, boolean[] key2) {
        boolean[] permut =  new boolean[8];
        permut[0] = key2[0];
        permut[1] = key[2];
        permut[2] = key2[1];
        permut[3] = key[3];
        permut[4] = key2[2];
        permut[5] = key[4];
        permut[6] = key2[4];
        permut[7] = key2[3];
        return permut;
    }

    private boolean[] circularLeftShift(boolean[] key, int bits){
        boolean[] temp = new boolean[5];
        int position = 0;
        for (int i=0; i<key.length; i++) {
            position = (i - bits) % key.length;
            if( position < 0) {
                position += key.length;
            }
            temp[position] = key[i];
        }
        return temp;
    }


    private boolean[] splitArray (boolean[] input, int size, int padding, int tabSize){
        boolean[] output = new boolean[tabSize];
        for (int i=padding; i<size; i++){
            output[i % tabSize] = input[i];
        }
        return output;
    }

    private ArrayList< boolean []> generateKeys ()
    {
        ArrayList<boolean[]> output = new ArrayList<>();
        // Phase 1: Permutation
        boolean[] new_key;
        new_key = p10(master_key);

        //Phase 2: Découpage en deux moitié
        boolean[] firstPart = splitArray(new_key, 5, 0, 5);
        boolean[] secondPart = splitArray(new_key, 10, 5, 5);

        //Phase 3: Shift k1
        boolean[] k1_firstPart = circularLeftShift(firstPart, 1);
        boolean[] k1_secondPart = circularLeftShift(secondPart, 1);

        //Phase 3: Shift k2
        boolean[] k2_firstPart = circularLeftShift(k1_firstPart, 2);
        boolean[] k2_secondPart = circularLeftShift(k1_secondPart, 2);

        boolean[] k1 = p8(k1_firstPart, k1_secondPart);
        boolean[] k2 = p8(k2_firstPart, k2_secondPart);

        output.add(k1);
        output.add(k2);

        return output;
    }



    // Algorithme S-DES

    //IP (k1, k2, k3, k4, k5, k6, k7, k8) = (k2, k6, k3, k1, k4, k8, k5, k7)
    private boolean [] ip ( boolean [] plainText)
    {
        boolean[] permut = new boolean[8];
        permut[0] = plainText[1];
        permut[1] = plainText[5];
        permut[2] = plainText[2];
        permut[3] = plainText[0];
        permut[4] = plainText[3];
        permut[5] = plainText[7];
        permut[6] = plainText[4];
        permut[7] = plainText[6];

        return permut;
    }

    //IP?1 (k1, k2, k3, k4, k5, k6, k7, k8) = (k4, k1, k3, k5, k7, k2, k8, k6)
    private boolean [] rip ( boolean [] permutedText )
    {
        boolean[] permut = new boolean[8];
        permut[0] = permutedText[3];
        permut[1] = permutedText[0];
        permut[2] = permutedText[2];
        permut[3] = permutedText[4];
        permut[4] = permutedText[6];
        permut[5] = permutedText[1];
        permut[6] = permutedText[7];
        permut[7] = permutedText[5];

        return permut;
    }

    //E/P (n1, n2, n3, n4) = (n4, n1, n2, n3, n2, n3, n4, n1)
    private boolean [] ep ( boolean [] input)
    {
        boolean[] temp = new boolean[8];
        temp[0] = input[3];
        temp[1] = input[0];
        temp[2] = input[1];
        temp[3] = input[2];
        temp[4] = input[1];
        temp[5] = input[2];
        temp[6] = input[3];
        temp[7] = input[0];
        return temp;
    }

    //E/P (n1, n2, n3, n4) ? K1(k11, k12, k13, k14, k15, k16, k17, k18)
    //(n4 ? k11, n1 ? k12, n2 ? k13, n3 ? k14, n2 ? k15, n3 ? k16, n4 ? k17, n1 ? k18)
    private boolean [] xor ( boolean [] a, boolean [] b)
    {
        boolean[] temp = new boolean[8];
        temp[0] = a[3] ^ b[0];
        temp[1] = a[0] ^ b[1];
        temp[2] = a[1] ^ b[2];
        temp[3] = a[2] ^ b[3];
        temp[4] = a[1] ^ b[4];
        temp[5] = a[2] ^ b[5];
        temp[6] = a[3] ^ b[6];
        temp[7] = a[0] ^ b[7];
        return temp;
    }

    private boolean[][][] initializeSBox_1 (boolean[][][] sbox){
        //// First row
        // S0(00,00)=01
        sbox[0][0][0]= false;
        sbox[0][0][1]= true;

        // S0(00,01)=00
        sbox[1][0][0]= false;
        sbox[1][0][1]= false;

        // S0(00,10)=11
        sbox[2][0][0]= true;
        sbox[2][0][1]= true;

        // S0(00,11)=10
        sbox[3][0][0]= true;
        sbox[3][0][1]= false;


        //Second Row
        // S0(01,00)=11
        sbox[0][1][0]= true;
        sbox[0][1][1]= true;

        // S0(01,01)=10
        sbox[1][1][0]= true;
        sbox[1][1][1]= false;

        // S0(01,10)=01
        sbox[2][1][0]= false;
        sbox[2][1][1]= true;

        // S0(01,11)=00
        sbox[3][1][0]= false;
        sbox[3][1][1]= false;

        ////Third row
        // S0(10,00)=00
        sbox[0][2][0]= false;
        sbox[0][2][1]= false;

        // S0(10,01)=10
        sbox[1][2][0]= true;
        sbox[1][2][1]= false;

        // S0(10,10)=01
        sbox[2][2][0]= false;
        sbox[2][2][1]= true;

        // S0(10,11)=11
        sbox[3][2][0]= true;
        sbox[3][2][1]= true;

        ////Fourth row
        // S0(11,00)=11
        sbox[0][3][0]= true;
        sbox[0][3][1]= true;

        // S0(11,01)=01
        sbox[1][3][0]= false;
        sbox[1][3][1]= true;

        // S0(11,10)=11
        sbox[2][3][0]= true;
        sbox[2][3][1]= true;

        // S0(11,11)=10
        sbox[3][3][0]= true;
        sbox[3][3][1]= false;

        return sbox;
    }

    private boolean[][][] initializeSBox_2 (boolean[][][] sbox){
        //// First row
        // S0(00,00)=00
        sbox[0][0][0]= false;
        sbox[0][0][1]= false;

        // S0(00,01)=01
        sbox[1][0][0]= false;
        sbox[1][0][1]= true;

        // S0(00,10)=10
        sbox[2][0][0]= true;
        sbox[2][0][1]= false;

        // S0(00,11)=11
        sbox[3][0][0]= true;
        sbox[3][0][1]= true;


        //Second Row
        // S0(01,00)=10
        sbox[0][1][0]= true;
        sbox[0][1][1]= false;

        // S0(01,01)=00
        sbox[1][1][0]= false;
        sbox[1][1][1]= false;

        // S0(01,10)=01
        sbox[2][1][0]= false;
        sbox[2][1][1]= true;

        // S0(01,11)=11
        sbox[3][1][0]= true;
        sbox[3][1][1]= true;

        ////Third row
        // S0(10,00)=11
        sbox[0][2][0]= true;
        sbox[0][2][1]= true;

        // S0(10,01)=00
        sbox[1][2][0]= false;
        sbox[1][2][1]= false;

        // S0(10,10)=01
        sbox[2][2][0]= false;
        sbox[2][2][1]= true;

        // S0(10,11)=00
        sbox[3][2][0]= false;
        sbox[3][2][1]= false;

        ////Fourth row
        // S0(11,00)=10
        sbox[0][3][0]= true;
        sbox[0][3][1]= false;

        // S0(11,01)=01
        sbox[1][3][0]= false;
        sbox[1][3][1]= true;

        // S0(11,10)=00
        sbox[2][3][0]= false;
        sbox[2][3][1]= false;

        // S0(11,11)=11
        sbox[3][3][0]= true;
        sbox[3][3][1]= true;

        return sbox;
    }

    private boolean[] useSBox(boolean[] a, boolean[][][] sbox){
        boolean [] output = new boolean[2];
        int ligne = 0;
        int col = 0;

        if(a[0] == true && a[3] == true) {
            ligne = 3;
        }else if(a[0] == false && a[3] == true){
            ligne = 1;
        }else if(a[0] == true && a[3] == false) {
            ligne = 2;
        }else {
            ligne = 0;
        }

        if(a[1] == true && a[2] == true) {
            col = 3;
        }else if(a[1] == false && a[2] == true){
            col = 1;
        }else if(a[1] == true && a[2] == false) {
            col = 2;
        }else {
            col = 0;
        }

        output[0] = sbox[ligne][col][0];
        output[1] = sbox[ligne][col][1];

        return output;
    }

    //P4 (s00, s01, s10, s11) = (s01, s11, s10, s00)
    private boolean [] p4 ( boolean [] part1 , boolean [] part2)
    {
        boolean[] temp = new boolean[4];
        temp[0] = part1[1];
        temp[1] = part2[1];
        temp[2] = part2[0];
        temp[3] = part1[0];

        return temp;
    }

    private boolean [] f( boolean [] right , boolean [] sk)
    {
        boolean[] ep = ep(right);
        boolean[] xor = xor(ep,sk);
        boolean[] firstArray = splitArray(xor, 4, 0, 4);
        boolean[] secondArray = splitArray(xor, 8, 4, 4);

        initializeSBox_1(s_box1);
        initializeSBox_2(s_box2);

        boolean[] out_sbox1 = useSBox(firstArray, s_box1);
        boolean[] out_sbox2 = useSBox(secondArray, s_box2);

        boolean[] out_p4 = p4(out_sbox1, out_sbox2);

        return out_p4;
    }

    private boolean [] fK ( boolean [] bits , boolean [] key)
    {
        boolean[] left = splitArray(bits, 4, 0, 4);
        boolean[] right = splitArray(bits, 8, 4, 4);

        boolean[] output_f =  f(right,key);
        boolean[] output_fk = new boolean[8];

        for (int i = 0; i < 4; i++) {
            output_fk[i] = left[i] ^ output_f[i];
        }

        for (int i = 0; i < 4; i++) {
            output_fk[i + 4] = right[i];
        }

        return output_fk;
    }

    private boolean [] sw ( boolean [] input)
    {
        boolean [] temp = new boolean[8];
        temp[0] = input[4];
        temp[1] = input[5];
        temp[2] = input[6];
        temp[3] = input[7];
        temp[4] = input[0];
        temp[5] = input[1];
        temp[6] = input[2];
        temp[7] = input[3];

        return temp;
    }

    public boolean[] toBinary(char letter){
        boolean[] output = new boolean[8];
        String data = Integer.toBinaryString((int) letter);
        int Alength = data.length();

        if(Alength < 8) {
            for (int i = 0; i < 8 - Alength; i++) {
                data = "0" + data;
            }
        }
        int index=0;
        for (char ch: data.toCharArray()){
            if(Character.getNumericValue(ch) == 1) {
                output[index] = true;
            } else {
                output[index] = false;
            }
            index++;
        }

        return output;
    }

    public char toChar(boolean[] bits){
        String data = "";
        for (int i = 0; i < 8; i++) {
            if(bits[i] == true) {
                data += "1";
            } else {
                data += "0";
            }
        }
        return (char)Integer.parseInt(data,2);
    }

    public char encrypt ( char block)
    {
        ArrayList<boolean[]> keys = generateKeys();
        boolean[] ip = ip(toBinary(block));
        boolean[] firstRound = fK(ip,keys.get(0));
        boolean[] sw = sw(firstRound);
        boolean[] secondRound = fK(sw, keys.get(1));
        boolean[] rip = rip(secondRound);

        return toChar(rip);
    }

    public char decrypt ( char block)
    {
        ArrayList<boolean[]> keys = generateKeys();
        boolean[] ip = ip(toBinary(block));
        boolean[] firstRound = (fK(ip,keys.get(1)));
        boolean[] sw = sw(firstRound);
        boolean[] secondRound = fK(sw, keys.get(0));
        boolean[] rip = rip(secondRound);

        return toChar(rip);

    }

}
