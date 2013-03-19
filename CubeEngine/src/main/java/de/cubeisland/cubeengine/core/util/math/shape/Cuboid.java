package de.cubeisland.cubeengine.core.util.math.shape;

import de.cubeisland.cubeengine.core.util.math.Vector3;
import de.cubeisland.cubeengine.core.util.math.shape.iterator.CuboidIterator;
import java.util.Iterator;

public class Cuboid implements Shape
{
    private Vector3 point;
    
    private double width;
    private double height;
    private double depth;
    
    private Vector3 rotationAngle;
    private Vector3 centerOfRotation;

    public Cuboid( Vector3 point, double width, double height, double depth )
    {
        this.point = point;
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        this.centerOfRotation = new Vector3(this.point.x + width / 2, this.point.y + height / 2, this.point.z + depth / 2);
        this.rotationAngle = new Vector3(0, 0, 0);
    }

    
    public Cuboid( Vector3 point, double width, double height, double depth, Vector3 centerOfRotation, Vector3 rotationAngle)
    {
        this.point = point;
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        this.centerOfRotation = centerOfRotation;
        this.rotationAngle = rotationAngle;
    }

    @Override
    public Shape setPoint( Vector3 point )
    {
        return new Cuboid(point, this.width, this.height, this.depth, this.centerOfRotation, this.rotationAngle);
    }
    
    @Override
    public Vector3 getPoint()
    {
        return this.point;
    }
    
    public Cuboid setWidth(double width)
    {
        return new Cuboid(this.point, width, this.height, this.depth, this.centerOfRotation, this.rotationAngle);
    }
    
    public double getWidth()
    {
        return this.width;
    }
    
    public Cuboid setHeight(double height)
    {
        return new Cuboid(this.point, this.width, height, this.depth, this.centerOfRotation, this.rotationAngle);
    }
    
    public double getHeight()
    {
        return this.height;
    }
    
    public Cuboid setDepth(double depth)
    {
        return new Cuboid(this.point, this.width, this.height, depth, this.centerOfRotation, this.rotationAngle);
    }
    
    public double getDepth()
    {
        return this.depth;
    }

    @Override
    public Shape rotate( Vector3 angle )
    {
        return new Cuboid(this.point, this.width, this.height, this.depth, this.centerOfRotation, angle);
    }

    @Override
    public Shape setCenterOfRotation( Vector3 center )
    {
        return new Cuboid(this.point, this.width, this.height, this.depth, center, this.rotationAngle);
    }

    @Override
    public Vector3 getRotationAngle()
    {
        return this.rotationAngle;
    }

    @Override
    public Vector3 getCenterOfRotation()
    {
        return this.centerOfRotation;
    }

    @Override
    public Shape scale( Vector3 vector )
    {
        return new Cuboid(this.point, this.width * vector.x, this.height * vector.y, this.depth * vector.z, this.centerOfRotation, this.rotationAngle);
    }
    
    private boolean intersects(Cuboid other)
    { 
        return !(                                                           // invert it
            this.getPoint().y + this.getHeight() < other.getPoint().y ||    // this.top < other.bottom
            this.getPoint().y > other.getPoint().y + other.getHeight()  ||  // this.bottom > other.top
            this.getPoint().x > other.getPoint().x + other.getWidth() ||    // this.left > other.right
            this.getPoint().x + this.getWidth() < other.getPoint().x ||     // this.right < other.left
            this.getPoint().z > other.getPoint().z + other.getDepth() ||    // this.front > other.back
            this.getPoint().z + this.getDepth() < other.getPoint().z ||     // this.back < other.front
            this.contains( other )
        );
    }

    @Override
    public boolean intersects( Shape other )
    {
        if(other instanceof Cuboid)
        {
            return this.intersects((Cuboid) other);
        }
        return false;  
    }

    private boolean contains(Cuboid other)
    {
        return 
        (
            this.getPoint().y + this.getHeight() > other.getPoint().y + other.getHeight() &&    // this.top > other.top
            this.getPoint().y < other.getPoint().y &&                                           // this.bottom < other.bottom
            this.getPoint().x < other.getPoint().x &&                                           // this.left < other.left
            this.getPoint().x + this.getWidth() > other.getPoint().x + other.getWidth() &&      // this.right > other.right
            this.getPoint().z < other.getPoint().z &&                                           // this.front < other.front
            this.getPoint().z + this.getDepth() > other.getPoint().z + other.getDepth()         // this.back > other.back
        );
    }
    
    @Override
    public boolean contains( Shape other )
    {
        if(other instanceof Cuboid)
        {
            return this.contains((Cuboid) other);
        }
        return false; 
    }

    @Override
    public Iterator<Vector3> iterator()
    {
        return new CuboidIterator( this );
    }
}