import java.util.Arrays;

/**
 * Created by Nokosi on 23/03/2017.
 */

public class TestSDES {
    public static void main(String [] args) {
        String key="1100101101";
        SDES my_Des = new SDES(key);

        String message =" hello world ";
        String message_enc ="";
        String message_dec ="";

        for(int i =0;i< message . length ();i++)
            message_enc += my_Des.encrypt(message .charAt(i));

        System.out.println(message);
        System.out.println (message_enc+"\n");

        for(int i =0;i< message . length ();i++)
            message_dec += my_Des.decrypt(message_enc.charAt(i));

        System.out.println (message_dec);



    }
}
