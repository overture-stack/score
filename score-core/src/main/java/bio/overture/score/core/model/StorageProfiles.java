package bio.overture.score.core.model;

import java.util.HashSet;
import java.util.Set;

public enum StorageProfiles {

    AZURE( "azure","az"),
    COLLABORATORY( "collaboratory","s3");



    private final String profileKey;
    private final String profileValue;

    StorageProfiles(String profileKey, String profileValue) {
        this.profileKey = profileKey;
        this.profileValue=profileValue;
    }

    public static String getProfileValue(String key){
        return StorageProfiles.valueOf(key.toUpperCase()).profileValue;
    }

    public static Set<String> keySet(){
        Set<String> valueSet = new HashSet<>();
        for ( StorageProfiles st : StorageProfiles.values()){
            valueSet.add(st.profileKey);
        }
        return valueSet;
    }

}
