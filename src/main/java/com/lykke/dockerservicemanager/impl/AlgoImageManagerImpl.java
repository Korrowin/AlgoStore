package com.lykke.dockerservicemanager.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.lykke.dockerservicemanager.api.AlgoImageManager;
import com.lykke.dockerservicemanager.exception.AlgoException;
import com.lykke.dockerservicemanager.exception.AlgoServiceManagerErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by niau on 10/23/17.
 */

@Component
public class AlgoImageManagerImpl implements AlgoImageManager {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    DockerClient dockerClient;

    @Override
    public String build(File dockerBaseDirPath, File dockerFilePath) throws  AlgoException{

        logger.debug("Building an image in + "+dockerBaseDirPath.getAbsolutePath()+ " with Dockerfile "+dockerFilePath.getAbsolutePath()+ "!");
            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    logger.debug("id:" + item.getId() + " status: " + item.getStatus());
                    super.onNext(item);
                }

                @Override
                public void onComplete() {
                    logger.debug("completed!");
                    super.onComplete();
                }
            };
        try {

            return dockerClient.buildImageCmd(dockerBaseDirPath).withDockerfile(dockerFilePath).exec(callback).awaitImageId();

        }catch (RuntimeException e){
           if (e.getMessage().contains("javac")){
               throw new AlgoException("Can't compile your code!!!", e, AlgoServiceManagerErrorCode.ALGO_COMPILATION_ERROR);

           }  else {
               throw new AlgoException(e.getMessage(), e, AlgoServiceManagerErrorCode.ALGO_CREATION_ERROR);
           }
        }

    }

    @Override
    public void pull(String repo, String tag) {
        logger.debug("Pulling an image from a repo: "+ repo+"with tag " +tag +"!");

            PullImageResultCallback callback = new PullImageResultCallback() {

                @Override
                public void onNext(PullResponseItem item) {
                    logger.debug("id:" + item.getId() + " status: " + item.getStatus());
                    super.onNext(item);
                }

                @Override
                public void onComplete() {
                    logger.debug("completed!");
                    super.onComplete();
                }

            };
        try {

            dockerClient.pullImageCmd(repo).exec(callback).awaitSuccess();
        }catch (RuntimeException e){
            throw new AlgoException(e.getMessage(), e, AlgoServiceManagerErrorCode.ALGO_PULL_ERROR);

        }
    }

    @Override
    public void tag(String image, String repo, String tag) {
        try {

            dockerClient.tagImageCmd(image, repo, tag).exec();
        }catch (RuntimeException e){
            throw new AlgoException(e.getMessage(),e,AlgoServiceManagerErrorCode.ALGO_TAG_ERROR);
        }


    }

    @Override
    public void push(String image, String repo, String tag) {
        logger.debug("Pushing an image "+image+" from a repo: "+ repo+"with tag " +tag +"!");


        PushImageResultCallback callback = new PushImageResultCallback() {


            @Override
            public void onNext(PushResponseItem item) {
                logger.debug("id:" + item.getId()  +" status: "+item.getStatus());
                super.onNext(item);
            }

        };
        try {
            dockerClient.pushImageCmd(image).withName(repo).withTag(tag).exec(callback).awaitCompletion(5, TimeUnit.MINUTES.MINUTES);
        } catch (Exception e) {
            throw new AlgoException(e.getMessage(),e,AlgoServiceManagerErrorCode.ALGO_SAVE_ERROR);
        }
    }

    @Override
    public List<Image> getImages() {
        List<Image> images = dockerClient.listImagesCmd().exec();
        return images;

    }
}
