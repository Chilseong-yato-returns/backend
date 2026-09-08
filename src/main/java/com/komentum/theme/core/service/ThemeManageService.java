package com.komentum.theme.core.service;

import com.komentum.designcomponent.domain.ColorStyle;
import com.komentum.designcomponent.domain.ComponentType;
import com.komentum.designcomponent.domain.DesignComponent;
import com.komentum.designcomponent.repository.ColorStyleRepository;
import com.komentum.global.exception.ResourceNotFoundException;
import com.komentum.theme.core.domain.ThemeComponent;
import com.komentum.theme.core.domain.ThemeImage;
import com.komentum.theme.core.domain.ThemeStyle;
import com.komentum.theme.core.dto.CreateThemeRequest;
import com.komentum.theme.core.dto.ThemeComponentDto;
import com.komentum.theme.core.mapper.ThemeComponentMapper;
import com.komentum.theme.core.mapper.ThemeImageMapper;
import com.komentum.theme.core.mapper.ThemeStyleMapper;
import com.komentum.theme.core.repository.ThemeComponentRepository;
import com.komentum.theme.core.repository.ThemeImageRepository;
import com.komentum.theme.core.repository.ThemeStyleRepository;
import com.komentum.user.domain.User;
import com.komentum.user.service.UserEntityFinder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThemeManageService {

  @PersistenceContext
  private EntityManager em;

  private final ThemeComponentRepository themeComponentRepository;
  private final ThemeStyleRepository themeStyleRepository;
  private final ThemeImageRepository themeImageRepository;
  private final ColorStyleRepository colorStyleRepository;
  private final ThemeComponentMapper themeComponentMapper;
  private final ThemeStyleMapper themeStyleMapper;
  private final ThemeImageMapper themeImageMapper;
  private final UserEntityFinder userEntityFinder;

  @Transactional
  void applyThemeImageAndStyle(ThemeComponent themeComponent, CreateThemeRequest request) {
    // 기존 theme image와 style 조회
    Set<ThemeImage> currentImages = themeComponent.getThemeImages();
    Set<ThemeStyle> currentStyles = themeComponent.getThemeStyles();
    // 요청으로 받은 theme image와 style을 Entity로 변환
    Set<ThemeImage> requestedImages = request.getImages().stream().map(ti -> {
      ComponentType componentType = em.getReference(ComponentType.class, ti.getComponentTypeId());
      DesignComponent designComponent = em.getReference(DesignComponent.class,
          ti.getDesignComponentId());
      return themeImageMapper.convertToTransientEntity(componentType, designComponent);
    }).collect(Collectors.toSet());
    Set<ThemeStyle> requestedStyles = request.getStyles().stream().map(ts -> {
      ColorStyle colorStyle = em.getReference(ColorStyle.class, ts.getColorStyleId());
      return themeStyleMapper.convertToTransientEntity(ts, colorStyle);
    }).collect(Collectors.toSet());
    // 기존 theme image / style에 있지만 요청받은 theme image / style에 없는 데이터 제거
    currentImages.retainAll(requestedImages);
    currentStyles.retainAll(requestedStyles);
    em.flush();
    // 요청 받은 theme image / style 중 현재 theme image / style에 있는 것을 제거하여 중복 제거
    requestedImages.removeAll(currentImages);
    requestedStyles.removeAll(currentStyles);
    // 새로운 theme image / style을 themeComponent에 추가
    requestedImages.forEach(themeComponent::addThemeImage);
    requestedStyles.forEach(themeComponent::addThemeStyle);
  }

  @Transactional
  public ThemeComponent createNewTheme(User targetUser) {
    String randomThemeName = "theme_" + UUID.randomUUID();
    return themeComponentRepository.save(
        ThemeComponent.builder()
            .themeName(randomThemeName)
            .userEmail(targetUser.getUserEmail())
            .versionName(randomThemeName + ".0.0.1")
            .versionNumber("1")
            .isPublic(true)
            .isDone(false)
            .build()
    );
  }

  /**
   * 지정된 이름으로 새로운 테마를 생성한다. 이름이 없으면 임의의 이름을 부여한다.
   *
   * @param targetUser 테마 제작자로 설정할 사용자
   * @param themeName  테마 이름, null이거나 공백이면 임의의 이름을 사용한다
   * @return 생성된 ThemeComponent
   */
  @Transactional
  public ThemeComponent createNewTheme(User targetUser, String themeName) {
    ThemeComponent themeComponent = createNewTheme(targetUser);
    if (themeName != null && !themeName.isBlank()) {
      themeComponent.setThemeName(themeName);
    }
    return themeComponent;
  }

  @Transactional
  public void deleteTheme(Integer id) {
    ThemeComponent themeComponent = themeComponentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Theme not found with id: " + id));
    // 먼저 관련된 테마 스타일과 테마 이미지를 삭제
    themeStyleRepository.deleteByThemeComponentId(id);
    themeImageRepository.deleteByThemeComponentId(id);
    // 그 다음 테마 컴포넌트를 삭제
    themeComponentRepository.delete(themeComponent);
  }

  @Transactional
  public ThemeComponentDto markAsDone(Integer id) {
    ThemeComponent themeComponent = themeComponentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Theme not found with id: " + id));
    themeComponent.setIsDone(true);
    themeComponentRepository.save(themeComponent);
    return themeComponentMapper.convertToDto(themeComponent);
  }
}
