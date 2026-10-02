package com.golda.patchertiktok;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * UI strings. Kept in code instead of Android resources so they work inside TikTok
 * without loading the module APK's resources. The language follows TikTok's app language.
 */
final class I18n {
    enum S {
        APP_NAME, SECTION_FEED, ADS, ADS_DESC, LIVE, LIVE_DESC, ACQUAINTANCES, ACQUAINTANCES_DESC,
        SEEKBAR, SEEKBAR_DESC, SECTION_SOCIAL, COMMENT_REPOSTS, COMMENT_REPOSTS_DESC, NEW_PROFILE,
        NEW_PROFILE_DESC, EXTRA_FEATURES, EXTRA_FEATURES_DESC, SECTION_SHARING, NO_WATERMARK,
        DOWNLOAD_ANY, DOWNLOAD_ANY_DESC, SCREENSHOTS, SCREENSHOTS_DESC, CLEAN_LINKS, CLEAN_LINKS_DESC,
        SECTION_REGION, REGION, REGION_DESC, REGION_OFF, SEARCH, SECTION_STREAKS, AUTO_STREAK,
        AUTO_STREAK_DESC, SECTION_ABOUT, VERSION, SOURCE_CODE, RESTART_HINT, RESTART, OFF, BACK
    }

    private static final Map<String, String[]> TABLES = new HashMap<>();

    static {
        TABLES.put("en", new String[]{
                "TiktokPatchXposed", "Feed",
                "Block ads", "Ads in the feed, at startup and in other video lists",
                "Hide LIVE", "Live streams in the feed and the LIVE button",
                "Hide “People you may know”", "Videos suggested from your contacts",
                "Seek bar on every video", "Including short videos",
                "Comments & profile",
                "Comment reposts", "Repost videos with your own comment",
                "New profile layout", "Left-aligned profile with banners",
                "Hidden TikTok features", "Long-press speed-up, comments to Favorites, audio comments and more",
                "Downloads & links",
                "Download without watermark",
                "Download any video", "Even if the author turned off downloads",
                "Allow screenshots", "Screenshots and screen recording everywhere",
                "Clean links", "Remove tracking parameters from copied links",
                "Region", "Region", "TikTok will see a SIM card from the selected country. Can unlock content that is unavailable where you are.",
                "Off", "Search",
                "Streaks",
                "Auto streaks", "Keeps existing streaks in private chats alive while TikTok is open",
                "About", "Version", "Source code", "Restart TikTok to apply the changes", "Restart",
                "Off", "Back"});
        TABLES.put("ru", new String[]{
                "TiktokPatchXposed", "Лента",
                "Блокировать рекламу", "В ленте, при запуске и в других списках видео",
                "Скрыть LIVE", "Трансляции в ленте и кнопку LIVE",
                "Скрыть «Вы можете знать»", "Видео, предложенные по вашим контактам",
                "Перемотка на всех видео", "Включая короткие ролики",
                "Комментарии и профиль",
                "Репосты с комментарием", "Делитесь видео со своим комментарием",
                "Новый вид профиля", "Профиль с выравниванием влево и баннерами",
                "Скрытые функции TikTok", "Ускорение по удержанию, комментарии в избранное, аудиокомментарии и другое",
                "Загрузки и ссылки",
                "Скачивание без водяного знака",
                "Скачивать любые видео", "Даже если автор запретил скачивание",
                "Разрешить скриншоты", "Скриншоты и запись экрана везде",
                "Чистые ссылки", "Убирать параметры отслеживания из скопированных ссылок",
                "Регион", "Регион", "TikTok увидит SIM-карту выбранной страны. Может открыть контент, недоступный в вашем регионе.",
                "Выкл.", "Поиск",
                "Огоньки",
                "Автоогоньки", "Поддерживает существующие огоньки в личных чатах, пока TikTok открыт",
                "О модуле", "Версия", "Исходный код", "Перезапустите TikTok, чтобы применить изменения", "Перезапустить",
                "Выкл.", "Назад"});
        TABLES.put("uk", new String[]{
                "TiktokPatchXposed", "Стрічка",
                "Блокувати рекламу", "У стрічці, під час запуску та в інших списках відео",
                "Приховати LIVE", "Трансляції у стрічці та кнопку LIVE",
                "Приховати «Ви можете знати»", "Відео, запропоновані за вашими контактами",
                "Перемотування на всіх відео", "Включно з короткими роликами",
                "Коментарі та профіль",
                "Репости з коментарем", "Діліться відео зі своїм коментарем",
                "Новий вигляд профілю", "Профіль з вирівнюванням ліворуч і банерами",
                "Приховані функції TikTok", "Прискорення утриманням, коментарі в обране, аудіокоментарі тощо",
                "Завантаження та посилання",
                "Завантаження без водяного знака",
                "Завантажувати будь-які відео", "Навіть якщо автор заборонив завантаження",
                "Дозволити знімки екрана", "Знімки та запис екрана всюди",
                "Чисті посилання", "Видаляти параметри відстеження зі скопійованих посилань",
                "Регіон", "Регіон", "TikTok бачитиме SIM-картку вибраної країни. Може відкрити контент, недоступний у вашому регіоні.",
                "Вимк.", "Пошук",
                "Вогники",
                "Автовогники", "Підтримує наявні вогники в особистих чатах, поки TikTok відкрито",
                "Про модуль", "Версія", "Вихідний код", "Перезапустіть TikTok, щоб застосувати зміни", "Перезапустити",
                "Вимк.", "Назад"});
        TABLES.put("de", new String[]{
                "TiktokPatchXposed", "Feed",
                "Werbung blockieren", "Im Feed, beim Start und in anderen Videolisten",
                "LIVE ausblenden", "Livestreams im Feed und die LIVE-Schaltfläche",
                "„Personen, die du kennen könntest“ ausblenden", "Videos, die anhand deiner Kontakte vorgeschlagen werden",
                "Suchleiste bei allen Videos", "Auch bei kurzen Videos",
                "Kommentare & Profil",
                "Reposts mit Kommentar", "Videos mit eigenem Kommentar reposten",
                "Neues Profillayout", "Linksbündiges Profil mit Bannern",
                "Versteckte TikTok-Funktionen", "Schneller per Gedrückthalten, Kommentare in Favoriten, Audiokommentare und mehr",
                "Downloads & Links",
                "Ohne Wasserzeichen herunterladen",
                "Jedes Video herunterladen", "Auch wenn der Ersteller Downloads deaktiviert hat",
                "Screenshots erlauben", "Screenshots und Bildschirmaufnahmen überall",
                "Saubere Links", "Tracking-Parameter aus kopierten Links entfernen",
                "Region", "Region", "TikTok sieht eine SIM-Karte aus dem gewählten Land. Kann Inhalte freischalten, die bei dir nicht verfügbar sind.",
                "Aus", "Suchen",
                "Flammen",
                "Auto-Flammen", "Hält bestehende Flammen in Privatchats aktiv, solange TikTok geöffnet ist",
                "Info", "Version", "Quellcode", "Starte TikTok neu, um die Änderungen zu übernehmen", "Neu starten",
                "Aus", "Zurück"});
        TABLES.put("es", new String[]{
                "TiktokPatchXposed", "Feed",
                "Bloquear anuncios", "En el feed, al iniciar y en otras listas de vídeos",
                "Ocultar LIVE", "Directos en el feed y el botón LIVE",
                "Ocultar «Personas que quizá conozcas»", "Vídeos sugeridos a partir de tus contactos",
                "Barra de progreso en todos los vídeos", "También en vídeos cortos",
                "Comentarios y perfil",
                "Reposts con comentario", "Republica vídeos con tu propio comentario",
                "Nuevo diseño de perfil", "Perfil alineado a la izquierda con banners",
                "Funciones ocultas de TikTok", "Acelerar manteniendo pulsado, comentarios en Favoritos, comentarios de audio y más",
                "Descargas y enlaces",
                "Descargar sin marca de agua",
                "Descargar cualquier vídeo", "Aunque el autor haya desactivado las descargas",
                "Permitir capturas de pantalla", "Capturas y grabación de pantalla en todas partes",
                "Enlaces limpios", "Quitar parámetros de seguimiento de los enlaces copiados",
                "Región", "Región", "TikTok verá una SIM del país seleccionado. Puede desbloquear contenido no disponible en tu región.",
                "Desactivado", "Buscar",
                "Rachas",
                "Rachas automáticas", "Mantiene las rachas existentes en chats privados mientras TikTok está abierto",
                "Acerca de", "Versión", "Código fuente", "Reinicia TikTok para aplicar los cambios", "Reiniciar",
                "Desactivado", "Atrás"});
        TABLES.put("pt", new String[]{
                "TiktokPatchXposed", "Feed",
                "Bloquear anúncios", "No feed, ao abrir e em outras listas de vídeos",
                "Ocultar LIVE", "Transmissões ao vivo no feed e o botão LIVE",
                "Ocultar “Pessoas que você talvez conheça”", "Vídeos sugeridos a partir dos seus contatos",
                "Barra de progresso em todos os vídeos", "Inclusive em vídeos curtos",
                "Comentários e perfil",
                "Reposts com comentário", "Republique vídeos com seu próprio comentário",
                "Novo layout de perfil", "Perfil alinhado à esquerda com banners",
                "Recursos ocultos do TikTok", "Acelerar ao manter pressionado, comentários nos Favoritos, comentários em áudio e mais",
                "Downloads e links",
                "Baixar sem marca d'água",
                "Baixar qualquer vídeo", "Mesmo que o autor tenha desativado os downloads",
                "Permitir capturas de tela", "Capturas e gravação de tela em todo lugar",
                "Links limpos", "Remover parâmetros de rastreamento dos links copiados",
                "Região", "Região", "O TikTok verá um chip do país selecionado. Pode liberar conteúdo indisponível na sua região.",
                "Desativado", "Pesquisar",
                "Sequências",
                "Sequências automáticas", "Mantém as sequências existentes em chats privados enquanto o TikTok está aberto",
                "Sobre", "Versão", "Código-fonte", "Reinicie o TikTok para aplicar as alterações", "Reiniciar",
                "Desativado", "Voltar"});
        TABLES.put("fr", new String[]{
                "TiktokPatchXposed", "Fil",
                "Bloquer les publicités", "Dans le fil, au démarrage et dans les autres listes de vidéos",
                "Masquer les LIVE", "Les lives dans le fil et le bouton LIVE",
                "Masquer « Personnes que vous pourriez connaître »", "Vidéos suggérées à partir de vos contacts",
                "Barre de progression partout", "Même sur les vidéos courtes",
                "Commentaires et profil",
                "Republications avec commentaire", "Republiez des vidéos avec votre propre commentaire",
                "Nouvelle mise en page du profil", "Profil aligné à gauche avec bannières",
                "Fonctions cachées de TikTok", "Accélérer par appui long, commentaires dans les Favoris, commentaires audio et plus",
                "Téléchargements et liens",
                "Télécharger sans filigrane",
                "Télécharger n'importe quelle vidéo", "Même si le créateur a désactivé les téléchargements",
                "Autoriser les captures d'écran", "Captures et enregistrement d'écran partout",
                "Liens propres", "Supprimer les paramètres de suivi des liens copiés",
                "Région", "Région", "TikTok verra une carte SIM du pays choisi. Peut débloquer du contenu indisponible dans votre région.",
                "Désactivé", "Rechercher",
                "Séries",
                "Séries automatiques", "Maintient les séries existantes dans les chats privés tant que TikTok est ouvert",
                "À propos", "Version", "Code source", "Redémarrez TikTok pour appliquer les modifications", "Redémarrer",
                "Désactivé", "Retour"});
        TABLES.put("it", new String[]{
                "TiktokPatchXposed", "Feed",
                "Blocca pubblicità", "Nel feed, all'avvio e in altri elenchi di video",
                "Nascondi LIVE", "Dirette nel feed e il pulsante LIVE",
                "Nascondi «Persone che potresti conoscere»", "Video suggeriti in base ai tuoi contatti",
                "Barra di avanzamento ovunque", "Anche nei video brevi",
                "Commenti e profilo",
                "Repost con commento", "Ripubblica video con un tuo commento",
                "Nuovo layout del profilo", "Profilo allineato a sinistra con banner",
                "Funzioni nascoste di TikTok", "Velocizza tenendo premuto, commenti nei Preferiti, commenti audio e altro",
                "Download e link",
                "Scarica senza filigrana",
                "Scarica qualsiasi video", "Anche se l'autore ha disattivato i download",
                "Consenti screenshot", "Screenshot e registrazione dello schermo ovunque",
                "Link puliti", "Rimuovi i parametri di tracciamento dai link copiati",
                "Regione", "Regione", "TikTok vedrà una SIM del paese selezionato. Può sbloccare contenuti non disponibili nella tua zona.",
                "Disattivato", "Cerca",
                "Serie",
                "Serie automatiche", "Mantiene attive le serie esistenti nelle chat private mentre TikTok è aperto",
                "Info", "Versione", "Codice sorgente", "Riavvia TikTok per applicare le modifiche", "Riavvia",
                "Disattivato", "Indietro"});
        TABLES.put("pl", new String[]{
                "TiktokPatchXposed", "Kanał",
                "Blokuj reklamy", "W kanale, przy uruchamianiu i na innych listach filmów",
                "Ukryj LIVE", "Transmisje na żywo w kanale i przycisk LIVE",
                "Ukryj „Osoby, które możesz znać”", "Filmy proponowane na podstawie kontaktów",
                "Pasek przewijania we wszystkich filmach", "Także w krótkich filmach",
                "Komentarze i profil",
                "Udostępnienia z komentarzem", "Udostępniaj filmy z własnym komentarzem",
                "Nowy wygląd profilu", "Profil wyrównany do lewej z banerami",
                "Ukryte funkcje TikToka", "Przyspieszanie przytrzymaniem, komentarze w Ulubionych, komentarze audio i inne",
                "Pobieranie i linki",
                "Pobieranie bez znaku wodnego",
                "Pobieraj dowolne filmy", "Nawet jeśli autor wyłączył pobieranie",
                "Zezwalaj na zrzuty ekranu", "Zrzuty i nagrywanie ekranu wszędzie",
                "Czyste linki", "Usuwaj parametry śledzące z kopiowanych linków",
                "Region", "Region", "TikTok zobaczy kartę SIM z wybranego kraju. Może odblokować treści niedostępne w Twoim regionie.",
                "Wył.", "Szukaj",
                "Serie",
                "Automatyczne serie", "Podtrzymuje istniejące serie w czatach prywatnych, gdy TikTok jest otwarty",
                "Informacje", "Wersja", "Kod źródłowy", "Uruchom ponownie TikToka, aby zastosować zmiany", "Uruchom ponownie",
                "Wył.", "Wstecz"});
        TABLES.put("tr", new String[]{
                "TiktokPatchXposed", "Akış",
                "Reklamları engelle", "Akışta, açılışta ve diğer video listelerinde",
                "LIVE'ı gizle", "Akıştaki canlı yayınlar ve LIVE düğmesi",
                "“Tanıyor olabileceğin kişiler”i gizle", "Kişilerine göre önerilen videolar",
                "Tüm videolarda ilerleme çubuğu", "Kısa videolar dahil",
                "Yorumlar ve profil",
                "Yorumlu yeniden paylaşım", "Videoları kendi yorumunla yeniden paylaş",
                "Yeni profil düzeni", "Bannerlı, sola hizalı profil",
                "Gizli TikTok özellikleri", "Basılı tutarak hızlandırma, Favorilere yorum ekleme, sesli yorumlar ve daha fazlası",
                "İndirmeler ve bağlantılar",
                "Filigransız indir",
                "Her videoyu indir", "Yaratıcı indirmeyi kapatmış olsa bile",
                "Ekran görüntüsüne izin ver", "Her yerde ekran görüntüsü ve kaydı",
                "Temiz bağlantılar", "Kopyalanan bağlantılardan izleme parametrelerini kaldır",
                "Bölge", "Bölge", "TikTok seçilen ülkenin SIM kartını görür. Bölgende kullanılamayan içeriklerin kilidini açabilir.",
                "Kapalı", "Ara",
                "Seriler",
                "Otomatik seriler", "TikTok açıkken özel sohbetlerdeki mevcut serileri sürdürür",
                "Hakkında", "Sürüm", "Kaynak kodu", "Değişiklikleri uygulamak için TikTok'u yeniden başlat", "Yeniden başlat",
                "Kapalı", "Geri"});
        TABLES.put("id", new String[]{
                "TiktokPatchXposed", "Feed",
                "Blokir iklan", "Di feed, saat dibuka, dan di daftar video lainnya",
                "Sembunyikan LIVE", "Siaran langsung di feed dan tombol LIVE",
                "Sembunyikan “Orang yang mungkin kamu kenal”", "Video yang disarankan dari kontakmu",
                "Bilah progres di semua video", "Termasuk video pendek",
                "Komentar & profil",
                "Repost dengan komentar", "Posting ulang video dengan komentarmu sendiri",
                "Tata letak profil baru", "Profil rata kiri dengan banner",
                "Fitur tersembunyi TikTok", "Percepat dengan tekan lama, komentar ke Favorit, komentar audio, dan lainnya",
                "Unduhan & tautan",
                "Unduh tanpa watermark",
                "Unduh video apa pun", "Meskipun kreator menonaktifkan unduhan",
                "Izinkan tangkapan layar", "Tangkapan dan rekaman layar di mana saja",
                "Tautan bersih", "Hapus parameter pelacakan dari tautan yang disalin",
                "Wilayah", "Wilayah", "TikTok akan melihat SIM dari negara yang dipilih. Dapat membuka konten yang tidak tersedia di wilayahmu.",
                "Nonaktif", "Cari",
                "Streak",
                "Streak otomatis", "Menjaga streak yang ada di obrolan pribadi selama TikTok terbuka",
                "Tentang", "Versi", "Kode sumber", "Mulai ulang TikTok untuk menerapkan perubahan", "Mulai ulang",
                "Nonaktif", "Kembali"});
    }

    private static volatile String[] current = TABLES.get("en");

    private I18n() { }

    static void use(Locale locale) {
        String language = locale == null ? "en" : locale.getLanguage();
        if ("in".equals(language)) language = "id";
        if ("be".equals(language) || "kk".equals(language)) language = "ru";
        // Other languages without a translation fall back to English.
        String[] table = TABLES.get(language);
        current = table != null ? table : TABLES.get("en");
    }

    static String get(S key) {
        String[] table = current;
        int index = key.ordinal();
        return index < table.length ? table[index] : TABLES.get("en")[index];
    }

    /** Fails fast in unit tests if a translation table is out of sync with {@link S}. */
    static Map<String, String[]> tables() { return TABLES; }
}
