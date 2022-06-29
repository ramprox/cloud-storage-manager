# Cloud storage manager

<details open=""><summary><h2>Описание</h2></summary>
   <div>
      Cloud Storage Manager - облачное хранилище, предназначенное для хранения, загрузки и скачивания файлов в/из удаленного сервера.
   </div>
   <p></p>
   <div align="center">
      <a href="https://user-images.githubusercontent.com/73485824/173136229-54e4b77d-a8d9-43f2-ab1f-9d7deb96fde2.png"><img src="https://user-images.githubusercontent.com/73485824/173136229-54e4b77d-a8d9-43f2-ab1f-9d7deb96fde2.png"></a>
   </div>
</details>
<details><summary><h2>Стэк технологий</h2></summary>
   <ul>
      <li>JDK 1.8</li>
      <li>Maven</li>
      <li>JavaFX</li>
      <li>Spring</li>
      <li>MySQL 5.7.11</li>
      <li>Netty 4.1.59.Final</li>
      <li>Driver JDBC MySQL v.8.0.23</li>
   </ul>
</details>
<details><summary><h2>Функциональность</h2></summary>
   <ul>
      <li>
         <div><strong>Аутентификация и регистрация нового пользователя в БД</strong></div>
         <div>
            Подключение клиента к серверу происходит путем выбора Меню -> Соединение -> Соединиться. Затем в зависимости от выбранных кнопок
            "Sign In" (аутентификация) или "Sign Up"(регистрация) происходит соответственно либо аутентификация либо регистрация. При попытке регистрации
            с уже существующим логином или при попытке входа с данными пользователя, который уже в сети выдается сообщение с ошибкой.
         </div>
      </li>
      <li>
         <div><strong>База данных для хранения зарегистрированных пользователей</strong></div>
         <div>Дамп базы данных находится в файле cloud_storage.sql папке resources в модуле server</div>
      </li>
      <li>
         <strong>Работа с файлами и директориями на удаленном сервере и локально:</strong>
         <ul>
            <li>
               <div><strong>Создание</strong></div>
               <div>
                  Создание файла происходит в текущей директории клиента или сервера после выбора соответствующей таблицы
                  и нажатия кнопки "Создать файл" в нижней части окна или сочетания клавиш "Ctrl+N". Создание директории происходит
                  в текущей директории клиента или сервера после выбора соответствующей таблицы и нажатия кнопки "Создать папку"
                  в нижней части окна или сочетания клавиш "Ctrl+D". Перед созданием появляется окно, в котором пользователь
                  должен ввести имя вновь создаваемого файла и подтвердить или отменить операцию.
               </div>
            </li>
            <li>
               <div><strong>Удаление</strong></div>
               <div>
                  Удаление происходит после выбора соответствующего файла или директории на клиентской (с левой стороны) или
                  серверной таблице (с правой стороны) и нажатия кнопки "Удалить" в нижней части окна или клавиши "Del". Перед
                  удалением появляется окно, в котором пользователь должен подтвердить операцию.
               </div>
            </li>
            <li>
               <div><strong>Переименование</strong></div>
               <div>
                  Переименование выбранного файла или директории в текущей директории клиента или сервера происходит после нажатия
                  кнопки "Переименовать" в нижней части окна или сочетания клавиш "Ctrl+R". Перед изменением появляется окно,
                  в котором пользователь вводит новое имя файла и подтверждает изменение имени файла.
               </div>
            </li>
            <li>
               <div><strong>Загрузка на сервер (upload)</strong></div>
               <div>
                  Загрузка файлов и директорий на сервер происходит после выбора соответствующих файлов в таблице клиента и нажатия
                  кнопки "Копировать" в нижней части окна или клавиши "F5". После нажатия появляется окно для подтверждения данной операции.
                  Загрузка происходит в два этапа. Сначала клиент отправляет запрос на сервер с информацией о загружаемых файлах (в том числе
                  с информацией о файлах, находящихся во всех подпапках выбранных файлов). На стороне сервера происходит создание всех файлов
                  и сервер отправляет клиенту сигнал готовности для загрузки. На стороне клиента отфильтровываются файлы с ненулевым размером и
                  после того как приходит от сервера сигнал готовности, начинается передача содержимого файлов.
               </div>
            </li>
            <li>
               <div><strong>Загрузка из сервера (download)</strong></div>
               <div>
                  Загрузка файлов и директорий из сервера происходит после выбора соответствующего файла в таблице сервера и нажатия
                  кнопки "Копировать" в нижней части окна или клавиши "F5" и подтверждения операции загрузки.
                  Загрузка происходит в два этапа. Сначала клиент отправляет запрос на сервер, содержащей пути загружаемых файлов. Сервер
                  в ответ формирует информацию об этих (в том числе с информацией о файлах, находящихся во всех подпапках выбранных файлов).
                  На стороне клиента происходит создание всех файлов и фильтрация файлов с ненулевыми размерами. Далее из оставшегося списка клиент в цикле
                  отправляет запросы на сервер, содеожащими пути файлов ненулевых размеров. Сервер в ответ начинает передавать файлы по частям.
               </div>
            </li>
            <li>
               <div><strong>Просмотр размера, даты создания и последнего обновления</strong></div>
               <div>
                  В таблицах соответствующих клиентcкой и серверной стороне реализовано по умолчанию отображение размера только
                  файлов, т.к. содержимое директорий может быть многоуровневым и
                  содержащим множество файлов. Но через контекстное меню выбранной директории (кроме родительской) можно получить информацию о размере
                  выбранной директории. Полученное значение отображается в колонке "Размер" в строке, соответствующей выбранной
                  директории.
               </div>
            <li>
               <div><strong>Поиск</strong></div>
               <div>
                  Для осуществления поиска файлов на стороне клиента или сервера над каждой из соответствующих таблиц расположены
                  текстовые поля для ввода искомых файлов. Поиск осуществляется начиная с текущей директории. Результаты поиска
                  отображаются в новом окне в виде отсортированного списка, содержащего пути к файлам. Имена файлов сопоставляются,
                  используя ситаксис <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-">"glob"</a>.
               </div>
            </li>
            <li>
               <div><strong>Сортировка по имени, размеру, дате создания и последнего обновления</strong></div>
               <div>
                  При запуске программы происходит сортировка по умолчанию по именам в порядке возрастания (сначала отображаются
                  директории, потом файлы). В каждой из таблиц, соответствующих стороне клиента или сервера реализована возможность
                  сортировки по имени, размеру, дате создания и дате изменения в порядке возрастания или убывания. Изменение
                  вида сортировки происходит при щелчке левой кнопкой мыши на заголовке соответствующих колонок таблиц.
               </div>
            </li>
         </ul>
      </li>
   </ul>
</details>
<details><summary><h2>Сборка и запуск приложения</h2></summary>
   <div>
      Для запуска приложения локально необходимо иметь следующие установленные приложения:
   </div>
   <ul>
      <li><a href="https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html">JDK</a> &ndash; v.1.8;</li>
      <li><a href="https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html">Maven</a> &ndash; v.3.8.1;</li>
      <li><a href="https://dev.mysql.com/doc/refman/5.7/en/installing.html">MySQL</a> &ndash; v.5.7.11;</li>
   </ul>
   &nbsp;&nbsp;&nbsp;&nbsp;После установки вышеуказанных программ необходимо:
   <ul>
      <li><a href="#git_clone">Склонировать репозиторий на локальный компьютер</a></li>
      <li><a href="#mvn-build">Запустить сборку проекта через Maven</a></li>
      <li><a href="#run-app">Запустить приложение</a></li>
  </ul>
   
   <a name="git_clone"><h3>Склонировать репозиторий на локальный компьютер:</h3></a>
   ```
   git clone https://github.com/ramprox/cloud-storage-manager
   ```
   <a name="mvn-build"><h3>Запустить сборку проекта через Maven</h3></a>
   ```
   cd cloud-storage-manager
   mvn clean install
   ```
   <a name="run-app"><h3>Запустить приложение</h3></a>
   Необходимо открыть два консольных окна.
   В одном окне перейти в папку /cloud-storage-manager/server/target и запустить сервер:
   ```
   java -jar server-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
   В другом окне перейти в папку /cloud-storage-manager/client/target и запустить клиента:
   ```
   java -jar client-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
</details>
<details><summary><h2>Структура папок</h2></summary>
   <table>
      <tr>
         <th>Директория</th>
         <th>Описание</th>
      </tr>
      <tr>
         <td>client</td>
         <td>Клиентская часть</td>
      </tr>
      <tr>
         <td>interop</td>
         <td>Модуль структур данных и сервисов, используемых и сервером и клиентом.</td>
      </tr>
      <tr>
         <td>server</td>
         <td>Серверная часть</td>
      </tr>
   </table>
</details>
