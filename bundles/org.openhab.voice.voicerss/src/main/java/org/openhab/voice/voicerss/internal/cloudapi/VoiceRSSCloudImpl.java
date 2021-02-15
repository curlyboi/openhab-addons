/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.voice.voicerss.internal.cloudapi;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the Cloud service from VoiceRSS. For more information,
 * see API documentation at http://www.voicerss.org/api .
 *
 * Current state of implementation:
 * <ul>
 * <li>All API languages supported</li>
 * <li>Only default voice supported with good audio quality</li>
 * <li>Only MP3, OGG and AAC audio formats supported</li>
 * <li>It uses HTTP and not HTTPS (for performance reasons)</li>
 * </ul>
 *
 * @author Jochen Hiller - Initial contribution
 * @author Laurent Garnier - add support for all API languages
 * @author Laurent Garnier - add support for OGG and AAC audio formats
 * @author cURLy bOi - add support for all API languages (again) and their respective voices
 */
public class VoiceRSSCloudImpl implements VoiceRSSCloudAPI {

    private final Logger logger = LoggerFactory.getLogger(VoiceRSSCloudImpl.class);

    private static final Set<String> SUPPORTED_AUDIO_FORMATS = Stream.of("MP3", "OGG", "AAC").collect(toSet());

    private static final Set<Locale> SUPPORTED_LOCALES = new HashSet<>();
    static {
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ar-eg"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ar-sa"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("bg-bg"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ca-es"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("cs-cz"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("da-dk"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("de-at"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("de-de"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("de-ch"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("el-gr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-au"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-ca"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-gb"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-ie"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-in"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("en-us"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("es-es"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("es-mx"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("fi-fi"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("fr-ca"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("fr-fr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("fr-ch"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("he-il"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("hi-in"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("hr-hr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("hu-hu"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("id-id"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("it-it"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ja-jp"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ko-kr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ms-my"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("nb-no"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("nl-be"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("nl-nl"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("pl-pl"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("pt-br"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("pt-pt"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ro-ro"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ru-ru"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("sk-sk"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("sl-si"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("sv-se"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("ta-in"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("th-th"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("tr-tr"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("vi-vn"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("zh-cn"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("zh-hk"));
        SUPPORTED_LOCALES.add(Locale.forLanguageTag("zh-tw"));
    }

    private static final Map<String, Set<String>> SUPPORTED_VOICES = new HashMap<>();
    static {
        SUPPORTED_VOICES.put("ar-eg", new HashSet<>(Arrays.asList("Oda")));
        SUPPORTED_VOICES.put("ar-sa", new HashSet<>(Arrays.asList("Salim")));
        SUPPORTED_VOICES.put("bg-bg", new HashSet<>(Arrays.asList("Dimo")));
        SUPPORTED_VOICES.put("ca-es", new HashSet<>(Arrays.asList("Rut")));
        SUPPORTED_VOICES.put("cs-cz", new HashSet<>(Arrays.asList("Josef")));
        SUPPORTED_VOICES.put("da-dk", new HashSet<>(Arrays.asList("Freja")));
        SUPPORTED_VOICES.put("de-at", new HashSet<>(Arrays.asList("Lukas")));
        SUPPORTED_VOICES.put("de-de", new HashSet<>(Arrays.asList("Hanna", "Lina", "Jonas")));
        SUPPORTED_VOICES.put("de-ch", new HashSet<>(Arrays.asList("Tim")));
        SUPPORTED_VOICES.put("el-gr", new HashSet<>(Arrays.asList("Neo")));
        SUPPORTED_VOICES.put("en-au", new HashSet<>(Arrays.asList("Zoe", "Isla", "Evie", "Jack")));
        SUPPORTED_VOICES.put("en-ca", new HashSet<>(Arrays.asList("Rose", "Clara", "Emma", "Mason")));
        SUPPORTED_VOICES.put("en-gb", new HashSet<>(Arrays.asList("Alice", "Nancy", "Lily", "Harry")));
        SUPPORTED_VOICES.put("en-ie", new HashSet<>(Arrays.asList("Oran")));
        SUPPORTED_VOICES.put("en-in", new HashSet<>(Arrays.asList("Eka", "Jai", "Ajit")));
        SUPPORTED_VOICES.put("en-us", new HashSet<>(Arrays.asList("Linda", "Amy", "Mary", "John", "Mike")));
        SUPPORTED_VOICES.put("es-es", new HashSet<>(Arrays.asList("Camila", "Sofia", "Luna", "Diego")));
        SUPPORTED_VOICES.put("es-mx", new HashSet<>(Arrays.asList("Juana", "Silvia", "Teresa", "Jose")));
        SUPPORTED_VOICES.put("fi-fi", new HashSet<>(Arrays.asList("Aada")));
        SUPPORTED_VOICES.put("fr-ca", new HashSet<>(Arrays.asList("Emile", "Olivia", "Logan", "Felix")));
        SUPPORTED_VOICES.put("fr-fr", new HashSet<>(Arrays.asList("Bette", "Iva", "Zola", "Axel")));
        SUPPORTED_VOICES.put("fr-ch", new HashSet<>(Arrays.asList("Theo")));
        SUPPORTED_VOICES.put("he-il", new HashSet<>(Arrays.asList("Rami")));
        SUPPORTED_VOICES.put("hi-in", new HashSet<>(Arrays.asList("Puja", "Kabir")));
        SUPPORTED_VOICES.put("hr-hr", new HashSet<>(Arrays.asList("Nikola")));
        SUPPORTED_VOICES.put("hu-hu", new HashSet<>(Arrays.asList("Mate")));
        SUPPORTED_VOICES.put("id-id", new HashSet<>(Arrays.asList("Intan")));
        SUPPORTED_VOICES.put("it-it", new HashSet<>(Arrays.asList("Bria", "Mia", "Pietro")));
        SUPPORTED_VOICES.put("ja-jp", new HashSet<>(Arrays.asList("Hina", "Airi", "Fumi", "Akira")));
        SUPPORTED_VOICES.put("ko-kr", new HashSet<>(Arrays.asList("Nari")));
        SUPPORTED_VOICES.put("ms-my", new HashSet<>(Arrays.asList("Aqil")));
        SUPPORTED_VOICES.put("nb-no", new HashSet<>(Arrays.asList("Marte", "Erik")));
        SUPPORTED_VOICES.put("nl-be", new HashSet<>(Arrays.asList("Daan")));
        SUPPORTED_VOICES.put("nl-nl", new HashSet<>(Arrays.asList("Lotte", "Bram")));
        SUPPORTED_VOICES.put("pl-pl", new HashSet<>(Arrays.asList("Julia", "Jan")));
        SUPPORTED_VOICES.put("pt-br", new HashSet<>(Arrays.asList("Marcia", "Ligia", "Yara", "Dinis")));
        SUPPORTED_VOICES.put("pt-pt", new HashSet<>(Arrays.asList("Leonor")));
        SUPPORTED_VOICES.put("ro-ro", new HashSet<>(Arrays.asList("Doru")));
        SUPPORTED_VOICES.put("ru-ru", new HashSet<>(Arrays.asList("Olga", "Marina", "Peter")));
        SUPPORTED_VOICES.put("sk-sk", new HashSet<>(Arrays.asList("Beda")));
        SUPPORTED_VOICES.put("sl-si", new HashSet<>(Arrays.asList("Vid")));
        SUPPORTED_VOICES.put("sv-se", new HashSet<>(Arrays.asList("Molly", "Hugo")));
        SUPPORTED_VOICES.put("ta-in", new HashSet<>(Arrays.asList("Sai")));
        SUPPORTED_VOICES.put("th-th", new HashSet<>(Arrays.asList("Ukrit")));
        SUPPORTED_VOICES.put("tr-tr", new HashSet<>(Arrays.asList("Omer")));
        SUPPORTED_VOICES.put("vi-vn", new HashSet<>(Arrays.asList("Chi")));
        SUPPORTED_VOICES.put("zh-cn", new HashSet<>(Arrays.asList("Luli", "Shu", "Chow", "Wang")));
        SUPPORTED_VOICES.put("zh-hk", new HashSet<>(Arrays.asList("Jia", "Xia", "Chen")));
        SUPPORTED_VOICES.put("zh-tw", new HashSet<>(Arrays.asList("Akemi", "Lin", "Lee")));
    }

    @Override
    public Set<String> getAvailableAudioFormats() {
        return SUPPORTED_AUDIO_FORMATS;
    }

    @Override
    public Set<Locale> getAvailableLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    public Set<String> getAvailableVoices() {
        // different locales support different voices, so let's list all here in one big set when no locale is provided
        Set<String> allvoxes = new HashSet<>();
        for (Set<String> langvoxes : SUPPORTED_VOICES.values()) {
            for (String langvox : langvoxes) {
                allvoxes.add(langvox);
            }
        }
        return allvoxes;
    }

    @Override
    public Set<String> getAvailableVoices(Locale locale) {
        String langtag = locale.toLanguageTag();
        if (SUPPORTED_VOICES.containsKey(langtag)) {
            return SUPPORTED_VOICES.get(langtag);
        }
        return new HashSet<>();
    }

    /**
     * This method will return an input stream to an audio stream for the given
     * parameters.
     *
     * It will do that using a plain URL connection to avoid any external
     * dependencies.
     */
    @Override
    public InputStream getTextToSpeech(String apiKey, String text, String locale, String audioFormat)
            throws IOException {
        String url = createURL(apiKey, text, locale, audioFormat);
        logger.debug("Call {}", url);
        URLConnection connection = new URL(url).openConnection();

        // we will check return codes. The service will ALWAYS return a HTTP
        // 200, but for error messages, it will return a text/plain format and
        // the error message in body
        int status = ((HttpURLConnection) connection).getResponseCode();
        if (HttpURLConnection.HTTP_OK != status) {
            logger.error("Call {} returned HTTP {}", url, status);
            throw new IOException("Could not read from service: HTTP code " + status);
        }
        if (logger.isTraceEnabled()) {
            for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                logger.trace("Response.header: {}={}", header.getKey(), header.getValue());
            }
        }
        String contentType = connection.getHeaderField("Content-Type");
        InputStream is = connection.getInputStream();
        // check if content type is text/plain, then we have an error
        if (contentType.contains("text/plain")) {
            byte[] bytes = new byte[256];
            is.read(bytes, 0, 256);
            // close before throwing an exception
            try {
                is.close();
            } catch (IOException ex) {
                logger.debug("Failed to close inputstream", ex);
            }
            throw new IOException(
                    "Could not read audio content, service return an error: " + new String(bytes, "UTF-8"));
        } else {
            return is;
        }
    }

    // internal

    /**
     * This method will create the URL for the cloud service. The text will be
     * URI encoded as it is part of the URL.
     *
     * It is in package scope to be accessed by tests.
     */
    private String createURL(String apiKey, String text, String locale, String audioFormat) {
        String encodedMsg;
        try {
            encodedMsg = URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logger.error("UnsupportedEncodingException for UTF-8 MUST NEVER HAPPEN! Check your JVM configuration!", ex);
            // fall through and use msg un-encoded
            encodedMsg = text;
        }
        return "http://api.voicerss.org/?key=" + apiKey + "&hl=" + locale + "&c=" + audioFormat
                + "&f=44khz_16bit_mono&src=" + encodedMsg;
    }
}
