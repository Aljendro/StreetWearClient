package alejandro.alvarado.com.streetwear.Network;

import alejandro.alvarado.com.streetwear.StreetWearMap;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by alejandro on 4/12/17.
 */


/* Singleton Instance of the retrofit network  */
public class RetrofitNetwork {

    private final StreetWearServer mService;

    private static final RetrofitNetwork ourInstance = new RetrofitNetwork();

    public static RetrofitNetwork getInstance() {
        return ourInstance;
    }

    private RetrofitNetwork() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://stark-bastion-36294.herokuapp.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = retrofit.create(StreetWearServer.class);
    }

    public StreetWearServer getService () {
        return mService;
    }
}
