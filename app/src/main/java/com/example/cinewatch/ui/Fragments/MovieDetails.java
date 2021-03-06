package com.example.cinewatch.ui.Fragments;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cinewatch.R;
import com.example.cinewatch.Utils.Constants;
import com.example.cinewatch.adapters.CastAdapter;
import com.example.cinewatch.databinding.MovieDetailsBinding;
import com.example.cinewatch.db.WishListMovie;
import com.example.cinewatch.model.Cast;
import com.example.cinewatch.model.Movie;
import com.example.cinewatch.model.Video;
import com.example.cinewatch.ui.dialog.VideoDialog;
import com.example.cinewatch.viewmodel.HomeViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;

import java.util.ArrayList;
import java.util.HashMap;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by Abhinav Singh on 09,June,2020
 */
@AndroidEntryPoint
public class MovieDetails extends Fragment {
    private static final String TAG = "MovieDetails";
    private MovieDetailsBinding binding;
    private HomeViewModel viewModel;
    private Integer movieId;
    private HashMap<String, String> queryMap;
    private String temp,videoId;
    private CastAdapter adapter;
    private ArrayList<Cast> castList;
    private int hour =0,min = 0;
    private  Movie mMovie;
    private Boolean inWishList = false;
    private ArrayList<Video> videos;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MovieDetailsBinding.inflate(inflater,container,false);
        View view = binding.getRoot();
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(MovieDetails.this).get(HomeViewModel.class);
        castList = new ArrayList<>();
        queryMap = new HashMap<>();

        MovieDetailsArgs args = MovieDetailsArgs.fromBundle(getArguments());
        movieId = args.getMovieId();


        observeData();

        queryMap.put("api_key", Constants.API_KEY);
        queryMap.put("page","1");
        queryMap.put("append_to_response","videos");

        viewModel.getMovieDetails(movieId,queryMap);
        viewModel.getCast(movieId,queryMap);

        binding.castRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL,false));
        adapter = new CastAdapter(getContext(),castList);
        binding.castRecyclerView.setAdapter(adapter);
        binding.moviePoster.setClipToOutline(true);
        binding.addToWishList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inWishList){
                    viewModel.deleteMovie(movieId);
                    binding.addToWishList.setImageResource(R.drawable.ic_playlist_add_black_24dp);
                    Toast.makeText(getContext(),"Removed from WishList.",Toast.LENGTH_SHORT).show();

                }
                else {
                    WishListMovie movie = new WishListMovie(mMovie.getId(),mMovie.getPoster_path(),mMovie.getOverview(),
                            mMovie.getRelease_date(),mMovie.getTitle(),mMovie.getBackdrop_path(),mMovie.getVote_count(),
                            mMovie.getRuntime());
                    viewModel.insertMovie(movie);
                    binding.addToWishList.setImageResource(R.drawable.ic_playlist_add_check_black_24dp);
                    Toast.makeText(getContext(),"Added to WishList.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.playTrailer.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onClick(View view) {
                if(videoId != null){
                    VideoDialog dialog = new VideoDialog(videoId);
                    dialog.show(getParentFragmentManager(),"Video Dialog");
                }
                else
                    Toast.makeText(getContext(),"Sorry trailer not found!",Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void isMovieInWishList(int movieId) {
        if(viewModel.getWishListMovie(movieId) != null) {
            binding.addToWishList.setImageResource(R.drawable.ic_playlist_add_check_black_24dp);
            inWishList = true;
        }
        else {
            binding.addToWishList.setImageResource(R.drawable.ic_playlist_add_black_24dp);
            inWishList = false;
        }
        binding.addToWishList.setVisibility(View.VISIBLE);
    }

    private void observeData() {
        viewModel.getMovie().observe(getViewLifecycleOwner(), new Observer<Movie>() {
            @Override
            public void onChanged(Movie movie) {
                mMovie = movie;
                Glide.with(getContext()).load(Constants.ImageBaseURL + movie.getPoster_path())
                        .centerCrop()
                        .into(binding.moviePoster);

                binding.movieName.setText(movie.getTitle());

                hour = movie.getRuntime()/60;
                min = movie.getRuntime()%60;
                binding.movieRuntime.setText(hour+"h "+min+"m");
                binding.moviePlot.setText(movie.getOverview());
                temp = "";
                for (int i = 0; i < movie.getGenres().size(); i++){
                    if(i ==  movie.getGenres().size() -1)
                        temp+= movie.getGenres().get(i).getName();
                    else
                        temp+= movie.getGenres().get(i).getName() + " | ";
                }

                binding.movieGenre.setText(temp);
                binding.playTrailer.setVisibility(View.VISIBLE);
                binding.movieCastText.setVisibility(View.VISIBLE);
                binding.moviePlotText.setVisibility(View.VISIBLE);
                isMovieInWishList(movieId);

                JsonArray array = movie.getVideos().getAsJsonArray("results");
                videoId = array.get(0).getAsJsonObject().get("key").getAsString();

            }
        });

        viewModel.getMovieCastList().observe(getViewLifecycleOwner(), new Observer<ArrayList<Cast>>() {
            @Override
            public void onChanged(ArrayList<Cast> actors) {
                Log.e(TAG, "onChanged: " + actors.size() );
                adapter.setCastList(actors);
                adapter.notifyDataSetChanged();

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
