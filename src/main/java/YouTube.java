import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Print a list of top 100 influencers in beauty category on Youtube
 *
 * @author Srishti Lal
 */
public class YouTube {

    static int channelnumber = 0;
    static Map<String, BigInteger> YoutubeScoreMap = new HashMap<String, BigInteger>();

//    static String nextPageToken;

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final String PROPERTIES_FILENAME = "youtube.properties";

    //Maximum number of search responses allowed in a single search query
    private static final long NUMBER_OF_CHANNELS_RETURNED = 50;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static com.google.api.services.youtube.YouTube youtube;

    public static void main(String[] args) {
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
        String nextPageToken = "";

        // Read the developer key from the properties file.
        Properties properties = new Properties();
        try {
            InputStream in = YouTube.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {

            /*
             * The page variable is used to increment the page
             * to view the next 50 results produced by the search
             * query.
             */
            for (int page = 1; page < 5; page++) {

                Credential credential = Auth.authorize(scopes, "localizations");
                youtube = new com.google.api.services.youtube.YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                        .setApplicationName("flamingo-youtube").build();

            //sets the search query term to beauty
            String queryTerm = "beauty";

            // Define the API request for retrieving search results.
            com.google.api.services.youtube.YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set Youtube developer key from the Google Developers Console
            String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include channels. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("channel");


//            search.setOrder("viewcount");

            if(page == 2)
                search.setPageToken("CDIQAA");
            else if(page == 2)
                    search.setPageToken("CGQQAA");
            else if(page == 3)
                    search.setPageToken("CJYBEAA");
            else if(page == 4)
                search.setPageToken("CMgBEAA");

            search.setMaxResults(NUMBER_OF_CHANNELS_RETURNED);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            List<String> channelIds = new ArrayList<String>();

            if (searchResultList != null) {
                // Merge video IDs
                for (SearchResult searchResult : searchResultList) {
                    channelIds.add(searchResult.getId().getChannelId());
                }

                Joiner stringJoiner = Joiner.on(',');
                String channelId = stringJoiner.join(channelIds);

                // Call the YouTube Data API's youtube.videos.list method to
                // retrieve the resources that represent the specified channels.
                com.google.api.services.youtube.YouTube.Channels.List listChannelsRequest = youtube.channels().list("snippet, statistics").setId(channelId);
                ChannelListResponse listResponse = listChannelsRequest.execute();
                List<Channel> channelList = listResponse.getItems();
                if (channelList != null) {
                    prettyPrint(channelList.iterator());
                }

            }
        }
            Map<String, BigInteger> sortedYoutubeScoreMap = sortByComparator(YoutubeScoreMap, FALSE);
            printMap(sortedYoutubeScoreMap);
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query YouTube query (String)
     */
    private static void prettyPrint(Iterator<Channel> iteratorChannelResults) {

        if (!iteratorChannelResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorChannelResults.hasNext()) {

            channelnumber++;
//            System.out.println(channelnumber);
            Channel singleChannel = iteratorChannelResults.next();
            BigInteger subscriberCount = singleChannel.getStatistics().getSubscriberCount();
            BigInteger commentCount = singleChannel.getStatistics().getCommentCount();
            BigInteger viewCount = singleChannel.getStatistics().getViewCount();
            BigInteger YoutubeScore = subscriberCount.add(commentCount).add(viewCount);
            String title = singleChannel.getSnippet().getTitle();
            YoutubeScoreMap.put(title, YoutubeScore);


        }




    }

    private static Map<String, BigInteger> sortByComparator(Map<String, BigInteger> unsortMap, final boolean order)
    {

        List<Map.Entry<String, BigInteger>> list = new LinkedList<Map.Entry<String, BigInteger>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, BigInteger>>()
        {
            public int compare(Map.Entry<String, BigInteger> o1,
                               Map.Entry<String, BigInteger> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, BigInteger> sortedMap = new LinkedHashMap<String, BigInteger>();
        for (Map.Entry<String, BigInteger> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, BigInteger> map)
    {
        BigInteger maxValue = Collections.max(map.values());
        System.out.println(maxValue);

        BigInteger minValue = Collections.min(map.values());
        System.out.println(minValue);

        int i =1;
                    System.out.println("\n-------------------------------------------------------------\n");
            System.out.println("TOP 100 INFLUENCERS - BEAUTY ON YOUTUBE");
                    System.out.println("\n-------------------------------------------------------------\n");

        System.out.println("Influencer \t \t  Index");

        for (Map.Entry<String, BigInteger> entry : map.entrySet())
        {

            //Finding the overall infuence index
            BigInteger v1 = entry.getValue().subtract(minValue);
            BigInteger v2 = maxValue.subtract(minValue);
            BigDecimal decimalv1 = new BigDecimal(v1);
            BigDecimal decimalv2 = new BigDecimal(v2);
            BigDecimal normalisedYoutubeScore = decimalv1.divide(decimalv2, 4, RoundingMode.HALF_UP);
            Float normalisedYoutubeScorePercent = normalisedYoutubeScore.floatValue()*100;
            String influencer = i + ". " + entry.getKey();
            System.out.printf( "%-32s %f\n" , influencer, normalisedYoutubeScorePercent);
            i++;

            //We only need the top 100 so we will break at the 101'th value
            if (i == 101)
            {break;}
        }
    }
}