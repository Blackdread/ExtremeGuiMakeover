/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.extreme.ui;

import com.extreme.MovieApp;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

public class MediaControl extends HBox {

    private final boolean repeat = false;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;

    public MediaControl() {
        getStylesheets().add(MovieApp.class.getResource("/controls.css").toExternalForm());

        getStyleClass().add("media-controls");

        setAlignment(Pos.CENTER);
        setPadding(new Insets(5, 10, 5, 10));

        Button playButton = FontAwesomeIconFactory.get().createIconButton(FontAwesomeIcon.PLAY);

        playButton.setOnAction(e -> {
            MediaPlayer mp = getMediaPlayer();

            Status status = mp.getStatus();

            if (status == Status.UNKNOWN || status == Status.HALTED) {
                // don't do anything in these states
                return;
            }

            if (status == Status.PAUSED
                    || status == Status.READY
                    || status == Status.STOPPED) {
                // rewind the movie if we're sitting at the end
                if (atEndOfMedia) {
                    mp.seek(mp.getStartTime());
                    atEndOfMedia = false;
                }
                mp.play();
            } else {
                mp.pause();
            }
        });

        InvalidationListener updateValues = it -> updateValues();

        mediaPlayerProperty().addListener((observable, oldMp, mp) -> {

            if (oldMp != null) {
                oldMp.currentTimeProperty().removeListener(updateValues);
                oldMp.setOnPlaying(null);
                oldMp.setOnPaused(null);
                oldMp.setOnReady(null);
                oldMp.setOnEndOfMedia(null);
            }

            if (mp != null) {
                mp.currentTimeProperty().addListener(updateValues);

                mp.setOnPlaying(() -> {
                    if (stopRequested) {
                        mp.pause();
                        stopRequested = false;
                    } else {
                        playButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PAUSE));
                    }
                });

                mp.setOnPaused(() -> {
                    playButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PLAY));
                });

                mp.setOnReady(() -> {
                    duration = mp.getMedia().getDuration();
                    updateValues();
                });

                mp.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
                mp.setOnEndOfMedia(() -> {
                    if (!repeat) {
                        playButton.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.PLAY));
                        stopRequested = true;
                        atEndOfMedia = true;
                    }
                });
            }
        });


        getChildren().add(playButton);
        // Add spacer
        Label spacer = new Label("   ");
        getChildren().add(spacer);

        // Add Time label
        Label timeLabel = new Label("Time: ");
        getChildren().add(timeLabel);

        // Add time slider
        timeSlider = new Slider();
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (timeSlider.isValueChanging()) {
                    // multiply duration by percentage calculated by slider position
                    getMediaPlayer().seek(duration.multiply(timeSlider.getValue() / 100.0));
                }
            }
        });
        getChildren().add(timeSlider);

        // Add Play label
        playTime = new Label();
        playTime.setPrefWidth(130);
        playTime.setMinWidth(50);
        getChildren().add(playTime);

        // Add the volume label
        Label volumeLabel = new Label("Vol: ");
        getChildren().add(volumeLabel);

        // Add Volume slider
        volumeSlider = new Slider();
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (volumeSlider.isValueChanging()) {
                    getMediaPlayer().setVolume(volumeSlider.getValue() / 100.0);
                }
            }
        });
        getChildren().add(volumeSlider);
    }

    private final ObjectProperty<MediaPlayer> mediaPlayer = new SimpleObjectProperty<>(this, "mediaPlayer");

    public ObjectProperty<MediaPlayer> mediaPlayerProperty() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer player) {
        mediaPlayer.set(player);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer.get();
    }

    protected void updateValues() {
        if (playTime != null && timeSlider != null && volumeSlider != null) {
            Platform.runLater(() -> {
                MediaPlayer mp = getMediaPlayer();

                Duration currentTime = mp.getCurrentTime();
                playTime.setText(formatTime(currentTime, duration));
                timeSlider.setDisable(duration.isUnknown());
                if (!timeSlider.isDisabled()
                        && duration.greaterThan(Duration.ZERO)
                        && !timeSlider.isValueChanging()) {
                    timeSlider.setValue(currentTime.divide(duration).toMillis()
                            * 100.0);
                }
                if (!volumeSlider.isValueChanging()) {
                    volumeSlider.setValue((int) Math.round(mp.getVolume()
                            * 100));
                }
            });
        }
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }
}