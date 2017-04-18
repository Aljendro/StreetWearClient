package alejandro.alvarado.com.streetwear.Network;

/**
 * Created by alejandro on 4/12/17.
 */
import com.google.android.gms.maps.model.LatLng;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.HashMap;
import java.util.List;

public interface StreetWearServer {

    @Multipart
    @POST("streetimages/upload")
    Call<RowsChanged> uploadImage(
            @Part("latitude") Double latitude,
            @Part("longitude") Double longitude,
            @Part MultipartBody.Part filePart);

    @GET("location/potholes")
    Call<List<LatLng>> getPotholes(@Query("latitude") Double latitude,
                                   @Query("longitude") Double longitude,
                                   @Query("radius") int radius);

    @POST("location/potholes")
    Call<RowsChanged> postPotholes(@Body HashMap<String, Double> body);

}

