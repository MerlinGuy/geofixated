/**
 *
 * Created By: Jeff Boehmer
 * Company: Ft. Collins Research
 * Website: www.ftcollinsresearch.org
 *          www.geofixated.org
 * Date: 10/3/13
 * Time: 8:52 AM
 */

Geof.menus = {
    main: [
        {
            name:'upload',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Go-Up-64.png',
            title:'Upload',
            x:0,
            y:0,
            type:'view'

        },{
            name:'use',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Applications-Engineering-64.png',
            title:'Use',
            x:1,
            y:0,
            type:'menu'
        },{
            name:'manage',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Preferences-System-64.png',
            title:'Manange',
            x:2,
            y:0,
            type:'menu'
        }
    ],
    manage: [
        {
            name:'access',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Security-Medium-64.png',
            title:'Access',
            x:0,
            y:0,
            type:'menu'
        },{
            name:'system',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Network-Server-64.png',
            title:'System',
            x:1,
            y:0,
            type:'menu'
        }

    ],
    access: [
        {
            name:'usr',
            strokeWidth:this.dftStrWidth,
            imageSrc:'usr-64.png',
            title:'Users',
            x:0,
            y:0,
            type:'panel'
        },{
            name:'authcode',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Certificate.png',
            title:'Authorization Codes',
            x:0,
            y:1,
            type:'panel'
        },{
            name:'ugroup',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Groups-64.png',
            title:'User Groups',
            x:1,
            y:0,
            type:'panel'
        },{
            name:'project',
            strokeWidth:this.dftStrWidth,
            imageSrc:'project_64.png',
            title:'Projects',
            x:1,
            y:1,
            type:'panel'
        },
        {
            name:'entity',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Table_gray.png',
            title:'Server Entities',
            x:2,
            y:0,
            type:'panel'
        }
    ],
    system: [
        {
            name:'session',
            strokeWidth:this.dftStrWidth,
            imageSrc:'session.png',
            title:'Sessions',
            x:0,
            y:0,
            type:'panel'

        },
        {
            name:'dbpool',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Database.png',
            title:'DB Connections',
            x:1,
            y:0,
            type:'panel'

        },{
            name:'logger',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Logviewer-64.png',
            title:'System Logs',
            x:0,
            y:1,
            type:'panel'
        },{
            name:'configuration',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Preferences-Desktop-64.png',
            title:'Server Config',
            x:2,
            y:0,
            type:'panel'
        },{
            name:'audit',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Applications-Office-64.png',
            title:'Request Log',
            x:3,
            y:0,
            type:'panel'
        },{
            name:'storage',
            strokeWidth:this.dftStrWidth,
            imageSrc:'storage.png',
            title:'Storage Locations',
            x:1,
            y:1,
            type:'panel'
        }
        ,{
            name:'encryption',
            strokeWidth:this.dftStrWidth,
            imageSrc:'encryption.png',
            title:'Encryption',
            x:2,
            y:1,
            type:'panel'
        }
    ],
    use: [
        {
            name:'search',
            strokeWidth:this.dftStrWidth,
            imageSrc:'search.png',
            title:'Searches',
            x:0,
            y:0,
            type:'view',
            viewfile:'search'
        },{
            name:'map',
            strokeWidth:this.dftStrWidth,
            imageSrc:'globe.png',
            title:'Map',
            x:1,
            y:0,
            type:'view',
            viewfile:'full',
            onload:null
        },{
            name:'timeline',
            strokeWidth:this.dftStrWidth,
            imageSrc:'time_date.png',
            title:'Timeline',
            x:2,
            y:0,
            type:'view',
            viewfile:'full'
        },{
            name:'datagrid',
            strokeWidth:this.dftStrWidth,
            imageSrc:'spreadsheet2.png',
            title:'Spreadsheet',
            x:3,
            y:0,
            type:'view',
            viewfile:'full'
        },{
            name:'thumbnail',
            strokeWidth:this.dftStrWidth,
            imageSrc:'Gnome-Preferences-Other-64.png',
            title:'Image Grid',
            x:0,
            y:1,
            type:'view',
            viewfile:'full'
        }
    ]
};
