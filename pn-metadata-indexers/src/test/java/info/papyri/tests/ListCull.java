package info.papyri.tests;

public class ListCull {

    /**
     * @param args
     */
    
    private static String [] src =
    {
        "BGU",
        "BKT",
        "CPR",
        "P.Aberd.",
        "P.Abinn.",
        "P.Achm.",
        "P.Adl.",
        "P.Alex.",
        "P.Alex.Giss.",
        "P.Amh.",
        "P.Ammon",
        "P.Amst.",
        "P.Anag.",
        "P.Ant.",
        "P.Apoll.",
        "P.Ashm.",
        "P.Athen.",
        "P.Athen.Xyla",
        "P.Aust.Herr.",
        "P.Babatha",
        "P.Bacch.",
        "P.Bad.",
        "P.Bal.",
        "P.Bas.",
        "P.Batav.",
        "P.Beatty",
        "P.Benaki",
        "P.Berl.Bibl.",
        "P.Berl.Bork.",
        "P.Berl.Brash.",
        "P.Berl.Frisk",
        "P.Berl.Leihg.",
        "P.Berl.Möller",
        "P.Berl.Salmen.",
        "P.Berl.Sarisch.",
        "P.Berl.Schmidt",
        "P.Berl.Thun.",
        "P.Berl.Zill.",
        "P.Bingen",
        "P.Bodl.",
        "P.Bodm.",
        "P.Bon.",
        "P.Bour.",
        "P.Brem.",
        "P.Brookl.",
        "P.Brux.",
        "P.Bub.",
        "P.Cair.Goodsp.",
        "P.Cair.Isid.",
        "P.Cair.Masp.",
        "P.Cair.Mich.",
        "P.Cair.Preis.",
        "P.Cair.Zen.",
        "P.Charite",
        "P.Chept.",
        "P.Chic.",
        "P.Chic.Haw.",
        "P.Choix",
        "P.Col.",
        "P.Coll.Youtie",
        "P.Congr.",
        "P.Corn.",
        "P.Corpus Revillout",
        "P.Customs",
        "P.David",
        "P.Diog.",
        "P.Dion.",
        "P.Diosk.",
        "P.Dryton",
        "P.Dubl.",
        "P.Dura",
        "P.Edfou",
        "P.Edg.",
        "P.Egerton",
        "P.Egger",
        "P.Ehevertr.",
        "P.Eirene",
        "P.Eleph.",
        "P.Eleph.Dem.",
        "P.Eleph.Wagner",
        "P.Enteux.",
        "P.Erasm.",
        "P.Erbstreit",
        "P.Erl.",
        "P.Erl.Diosp.",
        "P.Euphrates",
        "P.Fam.Tebt.",
        "P.Fam.Theb.",
        "P.Fay.",
        "P.Flor.",
        "P.Fouad",
        "P.Frankf.",
        "P.Freer",
        "P.Freib.",
        "P.Fuad I Univ.",
        "P.Gen.",
        "P.Genova",
        "P.Giss.",
        "P.Giss.Apoll",
        "P.Giss.Lit.",
        "P.Giss.Univ.",
        "P.Got.",
        "P.Grad.",
        "P.Graux",
        "P.Grenf.",
        "P.Gron.",
        "P.Gur.",
        "P.Hal.",
        "P.Hamb.",
        "P.Harr.",
        "P.Harrauer",
        "P.Haun.",
        "P.Hausw.",
        "P.Hawara",
        "P.Heid.",
        "P.Hels.",
        "P.Hercul.",
        "P.Herm.",
        "P.Herm.Landl.",
        "P.Hermias",
        "P.Hever",
        "P.Hib.",
        "P.Holm.",
        "P.Hombert",
        "P.Horak",
        "P.Iand.",
        "P.Iand.inv.",
        "P.IFAO",
        "P.Ital.",
        "P.Jena",
        "P.Kar.Goodsp.",
        "P.Kellis",
        "P.Köln",
        "P.Kroll",
        "P.Kron.",
        "P.Laur.",
        "P.Leeds Mus.",
        "P.Leid.",
        "P.Leid.Inst.",
        "P.Leipz.",
        "P.Leit.",
        "P.Libbey",
        "P.Lille",
        "P.Lips.",
        "P.Lond.",
        "P.Lond.Copt.",
        "P.Lond.Lit.",
        "P.Lonsdorfer",
        "P.Louvre",
        "P.Lund",
        "P.Marini",
        "P.Marm.",
        "P.Masada",
        "P.Matr.",
        "P.Mert.",
        "P.Meyer",
        "P.Mich.",
        "P.Mich.Aphrod.",
        "P.Mich.Mchl",
        "P.Michael.",
        "P.Mil.",
        "P.Mil.Congr.",
        "P.Mil.Vogl.",
        "P.Mon.Apollo",
        "P.Mon.Epiph.",
        "P.Monac.",
        "P.Münch.",
        "P.Murab-ba’ât",
        "P.NagHamm.",
        "P.Naqlun",
        "P.Neph.",
        "P.Ness.",
        "P.NYU",
        "P.Oslo",
        "P.Oxf.",
        "P.Oxy.",
        "P.Oxy.Astr.",
        "P.Oxy.Census",
        "P.Oxy.Descr.",
        "P.Oxy.Hels.",
        "P.PalauRib.",
        "P.Panop.",
        "P.Panop.Beatty",
        "P.Paramone",
        "P.Paris",
        "P.Petaus",
        "P.Petersb.",
        "P.Petr.",
        "P.Pher.",
        "P.Phil.",
        "P.PisaLit.",
        "P.Polit.Jud.",
        "P.Pommersf.",
        "P.Prag.",
        "P.Prag.Varcl",
        "P.Princ.",
        "P.Princ.Roll",
        "P.Quseir",
        "P.Rain.Cent.",
        "P.Rain.Unterricht",
        "P.Recueil",
        "P.Rein.",
        "P.Rev.",
        "P.Ross.Georg.",
        "P.Ryl.",
        "P.Ryl.Dem.",
        "P.Sakaon",
        "P.Sarap.",
        "P.Sarga",
        "P.Schub.",
        "P.Sel.Warga",
        "P.Select.",
        "PSI",
        "PSI.Com.",
        "PSI.Congr.",
        "PSI.Corr.",
        "PSI.Il.",
        "PSI.Od.",
        "P.Siegesfeier",
        "P.Siut",
        "P.Sorb.",
        "P.Soter.",
        "P.Stras.",
        "P.Stras.Dem.",
        "P.Tebt.",
        "P.Tebt.Tait",
        "P.Tebt.Wall",
        "P.Thead.",
        "P.Theon.",
        "P.Thmouis",
        "P.Thomas",
        "P.Tor.",
        "P.Tor.Amen.",
        "P.Tor.Botti",
        "P.Tor.Choach.",
        "P.Turner",
        "P.Ups.",
        "P.Ups.Frid",
        "P.Vars.",
        "P.Vat.Aphrod.",
        "P.Verpfründung.",
        "P.Vind.Bosw.",
        "P.Vind.Sal.",
        "P.Vind.Sijp.",
        "P.Vind.Tand.",
        "P.Vind.Worp",
        "P.Warr.",
        "P.Wash.Univ.",
        "P.Wisc.",
        "P.Würzb.",
        "P.Yadin",
        "P.Yale",
        "P.Yale Copt.",
        "P.Zauzich",
        "P.Zen.Dem.",
        "P.Zen.Pestm.",
        "SB",
        "UPZ",
        "O.Amst.",
        "O.Ashm.",
        "O.Ashm.Shelt.",
        "O.Bawit",
        "O.Bawit IFAO",
        "O.Berl.",
        "O.Bodl.",
        "O.Brit.Mus.Copt.",
        "O.Brux.",
        "O.BuDjem",
        "O.Buch.",
        "O.Cair.",
        "O.Camb.",
        "O.Claud.",
        "O.Deir el-Bahari",
        "O.Deiss.",
        "O.Douch",
        "O.Edfou",
        "O.Elkab",
        "O.Erem.",
        "O.Fay.",
        "O.Florida",
        "O.Hor",
        "O.Joach.",
        "O.Leid.",
        "O.Lund.",
        "O.Magnien",
        "O.Masada",
        "O.Medin.Madi",
        "O.Métrologie",
        "O.Mich.",
        "O.Minor",
        "O.Mon.Phoib.",
        "O.Nancy",
        "O.Narm.",
        "O.Oasis",
        "O.Ont.Mus.",
        "O.Oslo",
        "O.Paris",
        "O.Petr.",
        "O.Sarga",
        "O.Stras.",
        "O.Tebt.",
        "O.Tebt.Pad.",
        "O.Theb.",
        "O.Vleem.",
        "O.Wadi Hamm.",
        "O.Waqfa",
        "O.Wilb.",
        "O.Wilck.",
        "T.Alb.",
        "T.Mom.Louvre",
        "T.Varie",
        "C.Étiq.Mom.",
        "C.Illum.Pap.",
        "C.Jud.Syr.Eg.",
        "C.Pap.Gr.",
        "Chrest.Mitt.",
        "Chrest.Wilck.",
        "Doc.Eser.Rom.",
        "Feste",
        "Jur.Pap.",
        "Stud.Pal.",
        "P.Aberd.",
        "P.Alex.",
        "P.Brookl.",
        "P.Diog.",
        "P.Dura",
        "P.Fouad",
        "P.Grenf.",
        "P.Harrauer",
        "P.Haun.",
        "P.Iand.",
        "P.Ital.",
        "P.Lond.",
        "P.Marini",
        "P.Masada",
        "P.Mich.",
        "P.Michael.",
        "P.Murab-ba’ât",
        "P.Quseir",
        "P.Rain.Cent.",
        "P.Thomas",
        "O.BuDjem",
        "O.Florida",
        "O.Masada",
        "T.Dacia",
        "T.Jucundus",
        "T.Pizzaras",
        "T.Sulpicii",
        "T.Vindol.",
        "T.Vindon.",
        "Stud.Pal.",
        "Ch.L.A.",
        "P.Adl.",
        "P.Äg.Handschrift.",
        "P.Ashm.",
        "P.Assoc.",
        "P.Auswahl",
        "P.Bad.",
        "P.Batav.",
        "P.Berl.Dem.",
        "P.Berl.Spieg.",
        "P.Bingen",
        "P.Brit.Mus.",
        "P.Brit.Mus.Reich",
        "P.Brookl.Dem.",
        "P.Brux.Dem.",
        "P.Bürgsch.",
        "P.Cair.",
        "P.Carlsb.",
        "P.CattleDocs.",
        "P.Chic.Haw.",
        "P.Choach.Survey",
        "P.Choix",
        "P.Chrest.Nouvelle",
        "P.Chrest.Revillout",
        "P.Chronik",
        "P.Coll.Youtie",
        "P.Corpus Revillout",
        "P.David",
        "P.Demotica",
        "P.Dion",
        "P.Dryton",
        "P.Ehevertr.",
        "P.Eleph.Dem.",
        "P.Erbstreit",
        "P.Fam.Theb.",
        "P.Freib.",
        "P.Gebelen",
        "P.Harrauer",
        "P.Hausw.",
        "P.Hawara",
        "P.Hermias",
        "P.Horak",
        "P.Hou",
        "P.KölnÄg.",
        "P.Köln Lüddeckens",
        "P.Land Leases",
        "P.Leid.Dem.",
        "P.Lesestücke",
        "P.Libbey",
        "P.LilleDem.",
        "P.Loeb",
        "P.Lond.",
        "P.Louvre",
        "P.Lonsdorfer",
        "P.Mallawi",
        "P.Meerman.",
        "P.Meyer",
        "P.Mich.",
        "P.Mich.Nims",
        "P.Mil.Vogl.",
        "P.Oxford Griffith",
        "P.Prag.Satzung",
        "PSI",
        "P.QasrIbrim",
        "P.QuelquesTextes",
        "P.Rain.Cent.",
        "P.Recueil",
        "P.Rein",
        "P.Ryl.Dem.",
        "P.Schenkung.",
        "P.Schreibertrad.",
        "P.Siegesfeier",
        "P.Siut",
        "P.Slavery.Dem.",
        "P.Stras.Dem.",
        "P.Tebt.",
        "P.Tebt.Tait",
        "P.Testi Botti",
        "P.Thomas",
        "P.Tor.Amen.",
        "P.Tor.Botti",
        "P.Tor.Choach.",
        "P.Tsenhor",
        "P.Turner",
        "P.Verpfründung.",
        "P.Zauzich",
        "P.Zen.Dem.",
        "P.Zen.Pestm.",
        "SB",
        "O.Amst.",
        "O.Ashm.Shelton",
        "O.Bodl.",
        "O.Buch.",
        "O.Camb.",
        "O.Edfou",
        "O.Hor",
        "O.Joachim",
        "O.Leid.Dem.",
        "O.Louvre",
        "O.Magnien",
        "O.Mattha",
        "O.Medin.HabuDem.",
        "O.Métrologie",
        "O.Muzawwaqa",
        "O.Narm.Dem.",
        "O.Petr.",
        "O.Stras.",
        "O.Tempeleide",
        "O.Theb.",
        "O.Vleem.",
        "O.Wångstedt",
        "O.Wilb.",
        "O.Zürich",
        "C.Jud.Syr.Eg.",
        "Stud.Pal.",
        "BKU",
        "CPR",
        "P.Amh.",
        "P.Amh.Eg.",
        "P.Apoll.",
        "P.Athen.Xyla",
        "P.Bad.",
        "P.Bal.",
        "P.Bas.",
        "P.Bingen",
        "P.Brem.",
        "P.Carlsb.",
        "P.CLT",
        "P.Congr.XV",
        "P.Fay.Copt.",
        "P.Freer",
        "P.Harrauer",
        "P.Hermitage Copt.",
        "P.Horak",
        "P.Kellis",
        "P.Köln",
        "P.Köln Ägypt.",
        "P.KRU",
        "P.Laur.",
        "P.Lond.",
        "P.Lond.Copt.",
        "P.Mich.",
        "P.Mich.Copt.",
        "P.Mil.Vogl.",
        "P.Mon.Apollo",
        "P.Mon.Epiph.",
        "P.MorganLib.",
        "P.MoscowCopt.",
        "P.NagHamm.",
        "P.Neph.",
        "P.Paramone",
        "P.Pisentius",
        "P.Princ.",
        "P.Rain.Unterricht",
        "P.Rain.Unterricht Kopt.",
        "P.Revillout Copt.",
        "P.Ryl.Copt.",
        "P.Sarga",
        "P.Schutzbriefe",
        "P.Thomas",
        "P.Turner",
        "P.YaleCopt.",
        "SB",
        "SB Kopt.",
        "O.Amst.",
        "O.Ashm.Copt.",
        "O.Bawit",
        "O.Bawit IFAO",
        "O.Brit.Mus.Copt.",
        "O.Buch.",
        "O.Camb.",
        "O.Crum",
        "O.Crum ST",
        "O.Crum VC",
        "O.Deir el-Bahari",
        "O.Douch",
        "O.Kellis",
        "O.Medin.HabuCopt.",
        "O.Mich.Copt.",
        "O.Mich.Copt.Etmoulon",
        "O.Mon.Phoib.",
        "O.Nancy",
        "O.Ont.Mus.",
        "O.Petr.",
        "O.Theb.",
        "O.Vind.Copt.",
        "Stud.Pal."
        }        ;
    
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        System.out.println("{");
        java.util.Arrays.sort(src);
        String last = "";
        for (int i=0;i<src.length;i++){
                if (!last.equals(src[i])) System.out.println("\"" + src[i] + "\",");
                last = src[i];
        }
        System.out.println("};");

    }

}
