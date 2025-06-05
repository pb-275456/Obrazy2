# Aplikacja przetwarzająca obrazy
Aplikacja okienkowa służy do wykonywania prostych operacji na obrazach typu jpg. Użytkownik ma możliwość załadowania obrazu do aplikacji, a następnie wykonania operacji: progowanie, konturowanie, skalowanie, negatyw. Po wykonaniu operacji istnieje możliwość zapisania obrazu jako plik jpg na pulpicie.

### Operacje na obrazach:
<ul>
<li>Progowanie - zamiana obrazu na czarno-biały, a następnie wykonanie progowania, czyli ustalenie minimalnej lub maksymalnej wartości koloru dla każdego piksela, w zależności od wartości progu (1-255)</li>
<li>Negatyw - odwrócenie kolorów obrazu na przeciwny</li>
<li>Konturowanie - wydobucie konturów obrazu za pomoca operatora Sobela</li>
<li>Skalowanie - przeskalowanie obrazu do wybranej wielkości z zakresu 0-3000</li>  
</ul>

### Wybrane funcjonalności:
<ul>
  <li>Aplikacja posiada logi, zapisujące informacje o udanych operacjach oraz błędach</li>
  <li>Operacje negatywu, progowanie i konturowania są wykonywane równolegle przy użyciu foreach.parrallel</li>
  <li>Zapisywanie plików wraz ze sprawdzeniem czy plik o danej nazwie istnieje</li>
  <li>Walidacja danych wpisywanych przez użytkownika</li>
  <li>Komunikaty o błędach i pomyślnie zakończonych operacjach</li>
</ul>
