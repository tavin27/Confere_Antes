package com.confereantes.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RemoteAudioFetcher {

    private final long maxBytes;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public RemoteAudioFetcher(@Value("${call.audio.max-bytes:26214400}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public AudioFile fetch(String audioUrl) {
        URI uri = validateUri(audioUrl);
        validatePublicHost(uri.getHost());

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "audio/*,application/octet-stream")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream stream = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalArgumentException("A URL do áudio respondeu HTTP " + response.statusCode());
                }

                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (contentLength > maxBytes) {
                    throw new IllegalArgumentException("O áudio excede o limite de tamanho permitido");
                }

                byte[] content = stream.readNBytes(Math.toIntExact(maxBytes + 1));
                if (content.length == 0) {
                    throw new IllegalArgumentException("O arquivo de áudio está vazio");
                }
                if (content.length > maxBytes) {
                    throw new IllegalArgumentException("O áudio excede o limite de tamanho permitido");
                }

                return new AudioFile(content, detectFormat(uri, response));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível baixar o áudio informado", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("O download do áudio foi interrompido", exception);
        }
    }

    private URI validateUri(String audioUrl) {
        try {
            URI uri = URI.create(audioUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("A URL do áudio precisa ser HTTPS pública e válida");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("A URL do áudio precisa ser HTTPS pública e válida", exception);
        }
    }

    private void validatePublicHost(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                String ip = address.getHostAddress().toLowerCase(Locale.ROOT);
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || ip.startsWith("fc")
                        || ip.startsWith("fd")) {
                    throw new IllegalArgumentException("A URL do áudio não pode apontar para uma rede privada");
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível validar o host da URL do áudio", exception);
        }
    }

    private String detectFormat(URI uri, HttpResponse<?> response) {
        String path = uri.getPath().toLowerCase(Locale.ROOT);
        for (String format : new String[] { "wav", "mp3", "flac", "m4a", "ogg", "webm", "aac" }) {
            if (path.endsWith("." + format)) {
                return format;
            }
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (contentType.contains("wav")) return "wav";
        if (contentType.contains("mpeg") || contentType.contains("mp3")) return "mp3";
        if (contentType.contains("flac")) return "flac";
        if (contentType.contains("ogg")) return "ogg";
        if (contentType.contains("webm")) return "webm";
        if (contentType.contains("aac")) return "aac";
        if (contentType.contains("mp4") || contentType.contains("m4a")) return "m4a";

        throw new IllegalArgumentException("Formato de áudio não reconhecido; use WAV, MP3, FLAC, M4A, OGG, WEBM ou AAC");
    }
}
