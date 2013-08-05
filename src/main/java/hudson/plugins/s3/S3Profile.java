package hudson.plugins.s3;

import com.amazonaws.ClientConfiguration;
import hudson.FilePath;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.internal.Mimetypes;
import hudson.util.Secret;

public class S3Profile {
    private String name;
    private String accessKey;
    private Secret secretKey;
    private String proxyHost;
    private String proxyPort;
    private transient volatile AmazonS3Client client = null;
    private ClientConfiguration clientConfiguration = null;

    public S3Profile() {
    }

    @DataBoundConstructor
    public S3Profile(String name, String accessKey, String secretKey, String proxyHost, String proxyPort) {
        this.name = name;
        this.accessKey = accessKey;
        this.secretKey = Secret.fromString(secretKey);
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public final String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public final Secret getSecretKey() {
        return secretKey;
    }

    public final String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AmazonS3Client getClient() {
        if (client == null) {
            client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey.getPlainText()), getClientConfiguration());
        }
        return client;
    }

    private ClientConfiguration getClientConfiguration(){
        if (clientConfiguration == null) {
            clientConfiguration = new ClientConfiguration();
            if(proxyHost != null && proxyHost.length() > 0) {
                clientConfiguration.setProxyHost(proxyHost);
                clientConfiguration.setProxyPort(Integer.parseInt(proxyPort));
            }
        }
        return clientConfiguration;
    }

    public void check() throws Exception {
        getClient().listBuckets();
    }



    public void upload(String bucketName, FilePath filePath, List<MetadataPair> userMetadata, String storageClass) throws IOException, InterruptedException {
        if (filePath.isDirectory()) {
            throw new IOException(filePath + " is a directory");
        }

        final Destination dest = new Destination(bucketName,filePath.getName());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(Mimetypes.getInstance().getMimetype(filePath.getName()));
        metadata.setContentLength(filePath.length());
        if ((storageClass != null) && !"".equals(storageClass)) {
            metadata.setHeader("x-amz-storage-class", storageClass);
        }
        for (MetadataPair metadataPair : userMetadata) {
            metadata.addUserMetadata(metadataPair.key, metadataPair.value);
        }
        try {
            getClient().putObject(dest.bucketName, dest.objectName, filePath.read(), metadata);
        } catch (Exception e) {
            throw new IOException("put " + dest + ": " + e);
        }
    }
}
