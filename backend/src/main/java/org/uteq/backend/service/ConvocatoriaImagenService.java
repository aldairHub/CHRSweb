package org.uteq.backend.service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.repository.ConvocatoriaRepository;
@Service
@RequiredArgsConstructor
public class ConvocatoriaImagenService {
    private final PromptBuilderService promptBuilder;
    private final HuggingFaceImagenService huggingFaceService;
    private final SupabaseStorageService supabaseService;
    private final ConvocatoriaRepository convocatoriaRepository;

    @Value("${huggingface.imagen.bucket}") private String bucket;

    public String generarYPersistir(Long id) throws Exception {
        Convocatoria conv = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convocatoria no encontrada: " + id));

        String prompt = promptBuilder.construir(conv);
        byte[] bytes = huggingFaceService.generarImagen(prompt);
        String url = supabaseService.subirBytes(bytes, "portadas", "conv_" + id, ".jpg", bucket);

        conv.setImagenPortadaUrl(url);
        convocatoriaRepository.save(conv);
        return url;
    }
}