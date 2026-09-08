package com.komentum.theme.core.service;

import com.google.common.base.Functions;
import com.komentum.designcomponent.domain.ColorStyle;
import com.komentum.designcomponent.enums.PlatformScope;
import com.komentum.designcomponent.enums.StyleCode;
import com.komentum.designcomponent.service.ColorStyleService;
import com.komentum.global.exception.ResourceNotFoundException;
import com.komentum.theme.core.domain.ThemeComponent;
import com.komentum.theme.core.domain.ThemeStyle;
import com.komentum.theme.core.dto.ThemeDetailResponse.StyleCodeInfo;
import com.komentum.theme.core.dto.ThemeUpdateRequest.ThemeStyleUpdateRequest;
import com.komentum.theme.core.repository.ThemeStyleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeStyleService {

  private final ThemeStyleRepository themeStyleRepository;
  private final ColorStyleService colorStyleService;

  @Transactional(readOnly = true)
  public Map<StyleCode, StyleCodeInfo> findStyleCodeMapByThemeComponentId(
      Integer themeComponentId) {
    List<ThemeStyle> themeStyles = themeStyleRepository.fetchJoinAllByThemeComponentId(
        themeComponentId);
    Map<StyleCode, StyleCodeInfo> res = themeStyles.stream()
        .collect(Collectors.toMap(
            ts -> ts.getColorStyle().getStyleCode(),
            ts -> StyleCodeInfo.of(ts.getColor(), ts.getAlpha(), ts.getColorStyle().getPlatformScope())
        ));
    if (res.size() != StyleCode.values().length) {
      log.warn(
          "[ThemeStyleService] Missing StyleCode. themeComponentId={}, actual={}, expected={}",
          themeComponentId,
          res.size(),
          StyleCode.values().length
      );
    }
    return res;
  }

  @Transactional(readOnly = true)
  public List<ThemeStyle> findByThemeComponentId(Integer themeComponentId) {
    return themeStyleRepository.findByThemeComponentId(themeComponentId);
  }

  @Transactional(readOnly = true)
  public List<ThemeStyle> fetchJoinThemeStylesByThemeComponentId(Integer themeComponentId) {
    return themeStyleRepository.fetchJoinAllByThemeComponentId(themeComponentId);
  }

  @Transactional
  public void copyThemeStyles(ThemeComponent targetTheme, ThemeComponent sourceTheme) {
    List<ThemeStyle> targetThemeStyles = new ArrayList<>();
    Set<ThemeStyle> sourceThemeStyles = sourceTheme.getThemeStyles();
    for (ThemeStyle sourceThemeStyle : sourceThemeStyles) {
      ThemeStyle targetThemeStyle = ThemeStyle.copyOf(targetTheme, sourceThemeStyle);
      targetThemeStyles.add(targetThemeStyle);
      targetTheme.addThemeStyle(targetThemeStyle);
    }
    themeStyleRepository.saveAll(targetThemeStyles);
  }

  /**
   * styleCode별 색상 정보로 새로운 ThemeStyle들을 생성하여 targetTheme에 추가한다.
   * platformScope=COMMON인 StyleCode는 요청에 반드시 포함되어야 하며, 특정 플랫폼 전용(ANDROID/IOS) StyleCode는
   * 요청에 없어도 된다. 요청에 포함된 StyleCode는 targetTheme에 아직 스타일이 없는 상태여야 한다.
   *
   * @param targetTheme 스타일을 추가할 대상 테마
   * @param styleCodes  styleCode별 색상 정보 맵, 존재하는 값은 null이 아니어야 한다
   * @throws ResponseStatusException   platformScope=COMMON인 StyleCode가 요청에 누락된 경우 (400 Bad Request)
   * @throws ResourceNotFoundException styleCode에 대응하는 ColorStyle이 존재하지 않는 경우
   * @throws IllegalArgumentException  요청 항목의 color 또는 alpha가 null이거나 alpha가 0~100 범위를 벗어난 경우
   */
  @Transactional
  public void createThemeStyles(
      ThemeComponent targetTheme,
      Map<StyleCode, ThemeStyleUpdateRequest> styleCodes
  ) {
    Map<StyleCode, ColorStyle> colorStyleMap = colorStyleService.findColorStyleMap();
    validateCommonStyleCodesPresent(colorStyleMap, styleCodes);
    List<ThemeStyle> themeStyles = new ArrayList<>();
    styleCodes.forEach((styleCode, styleRequest) -> {
      ColorStyle colorStyle = colorStyleMap.get(styleCode);
      if (colorStyle == null) {
        throw new ResourceNotFoundException("ColorStyle not found for styleCode: " + styleCode);
      }
      ThemeStyle themeStyle = ThemeStyle.builder()
          .themeComponent(targetTheme)
          .colorStyle(colorStyle)
          .build();
      themeStyle.updateColorAndAlpha(styleRequest.getColor(), styleRequest.getAlpha());
      themeStyles.add(themeStyle);
      targetTheme.addThemeStyle(themeStyle);
    });
    themeStyleRepository.saveAll(themeStyles);
  }

  /**
   * platformScope=COMMON인 StyleCode가 요청에 누락 없이 포함되어 있는지 검증한다.
   * 특정 플랫폼 전용(ANDROID/IOS) StyleCode는 검증 대상이 아니다.
   *
   * @throws ResponseStatusException 누락된 COMMON StyleCode가 있는 경우 (400 Bad Request)
   */
  private void validateCommonStyleCodesPresent(
      Map<StyleCode, ColorStyle> colorStyleMap,
      Map<StyleCode, ThemeStyleUpdateRequest> styleCodes
  ) {
    Set<StyleCode> missingCommonStyleCodes = colorStyleMap.entrySet().stream()
        .filter(entry -> entry.getValue().getPlatformScope() == PlatformScope.COMMON)
        .map(Entry::getKey)
        .filter(styleCode -> styleCodes.get(styleCode) == null)
        .collect(Collectors.toSet());
    if (!missingCommonStyleCodes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "common StyleCode(s) missing in request: " + missingCommonStyleCodes);
    }
  }

  /**
   * 요청으로 전달된 StyleCode별 color/alpha 값으로 기존 ThemeStyle을 덮어쓴다.
   * color와 alpha는 항상 함께 존재해야 하며, updateRequestMap이 비어있으면 아무 것도 하지 않는다.
   *
   * @throws ResourceNotFoundException 요청에 포함된 StyleCode에 대응하는 ThemeStyle이 존재하지 않는 경우
   * @throws IllegalArgumentException  요청 항목의 color 또는 alpha가 null이거나 alpha가 0~100 범위를 벗어난 경우
   */
  @Transactional
  public void updateThemeStyles(
      int themeComponentId,
      Map<StyleCode, ThemeStyleUpdateRequest> updateRequestMap) {
    if (updateRequestMap == null || updateRequestMap.isEmpty()) {
      return;
    }
    Map<StyleCode, ThemeStyle> themeStyleMap = themeStyleRepository
        .fetchJoinAllByThemeComponentId(themeComponentId)
        .stream().collect(Collectors.toMap(
            ts -> ts.getColorStyle().getStyleCode(),
            Functions.identity())
        );
    updateRequestMap.forEach((styleCode, updateRequest) -> {
      if (updateRequest == null) {
        throw new IllegalArgumentException("style update request must not be null: " + styleCode);
      }
      ThemeStyle themeStyle = themeStyleMap.get(styleCode);
      if (themeStyle == null) {
        throw new ResourceNotFoundException(
            "ThemeStyle not found for styleCode=" + styleCode
                + ", themeComponentId=" + themeComponentId);
      }
      themeStyle.updateColorAndAlpha(updateRequest.getColor(), updateRequest.getAlpha());
    });
  }
}
